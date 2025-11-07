#!/usr/bin/env bash
# If we're not running under bash (e.g., zsh), re-exec under bash.
if [ -z "${BASH_VERSION:-}" ]; then exec /usr/bin/env bash "$0" "$@"; fi

set -euo pipefail

# -------- CONFIG --------
SRC_ROOT="src/main/java"
BASE_OK_PKG="com.project.back_end"     # keep this
BAD_PKG_1="com.project_back_end"       # replace this -> com.project.back_end
BAD_PKG_2="com.project_back_end_shared" # drop or relocate
DTO_DIR_UPPER="DTO"
DTO_DIR_LOWER="dto"
REPOS_DIR="repositories"
SERVICES_DIR="services"
MODELS_DIR="models"
POM_FILE="pom.xml"

# -------- UTILS --------
has() { command -v "$1" >/dev/null 2>&1; }
gr() { if has rg; then rg "$@"; else grep -R "$@"; fi }
sed_i () {
  # cross-platform in-place sed
  if sed --version >/dev/null 2>&1; then
    sed -i "$@"
  else
    sed -i '' "$@"
  fi
}
say() { printf "\n\033[1;36m# %s\033[0m\n" "$*"; }

# -------- PRECHECKS --------
say "Prechecks"
test -f "$POM_FILE" || { echo "Run this from the project root (pom.xml not found)"; exit 1; }
test -d "$SRC_ROOT" || { echo "Java src folder $SRC_ROOT not found"; exit 1; }
if has git; then
  say "Creating backup branch 'fix/auto-refactor'"
  git add -A || true
  git commit -m "WIP before auto-refactor" || true
  git checkout -b fix/auto-refactor || git checkout fix/auto-refactor
else
  say "git not found (continuing without VCS). Make a backup zip if you want."
fi

# -------- 1) FIX PACKAGE NAMES IN SOURCE --------
say "Fixing package/import names (underscores -> dots)"
gr -n "$BAD_PKG_1" "$SRC_ROOT" || true
gr -n "$BAD_PKG_2" "$SRC_ROOT" || true
find "$SRC_ROOT" -type f -name "*.java" -print0 | xargs -0 perl -0777 -pe "s/$BAD_PKG_1/$BASE_OK_PKG/g" -i

say "Removing 'shared' imports (missing module) and listing TODOs"
gr -n "$BAD_PKG_2" "$SRC_ROOT" && echo "TODO: Move these shared classes into this project or delete imports." || true
if gr -n "$BAD_PKG_2" "$SRC_ROOT" >/dev/null 2>&1; then
  find "$SRC_ROOT" -type f -name "*.java" -print0 \
    | xargs -0 sed_i "s/import ${BAD_PKG_2}\./\/\/ TODO: missing shared module: import ${BAD_PKG_2}./g"
fi

# -------- 2) FIX FOLDERS THAT MATCH PACKAGE --------
say "Renaming folders to match '$BASE_OK_PKG'"
find "$SRC_ROOT" -type d -name "project_back_end" | while read -r d; do
  parent="$(dirname "$d")"
  target="$parent/project/back_end"
  mkdir -p "$parent/project"
  say "Moving $d -> $target"
  if has git; then git mv "$d" "$target" || { mv "$d" "$target"; }; else mv "$d" "$target"; fi
done

# -------- 3) DTO FOLDER CASE & PACKAGE --------
say "Standardizing DTO folder and package to '.dto'"
if [ -d "$SRC_ROOT/com/project/back_end/$DTO_DIR_UPPER" ]; then
  tmp="$SRC_ROOT/com/project/back_end/${DTO_DIR_UPPER}_tmp_move"
  mv "$SRC_ROOT/com/project/back_end/$DTO_DIR_UPPER" "$tmp"
  mkdir -p "$SRC_ROOT/com/project/back_end/$DTO_DIR_LOWER"
  shopt -s nullglob
  for f in "$tmp"/*.java; do
    base="$(basename "$f")"
    mv "$f" "$SRC_ROOT/com/project/back_end/$DTO_DIR_LOWER/$base"
  done
  rmdir "$tmp" || true
fi
if [ -d "$SRC_ROOT/com/project/back_end/$DTO_DIR_LOWER" ]; then
  find "$SRC_ROOT/com/project/back_end/$DTO_DIR_LOWER" -type f -name "*.java" -print0 \
    | xargs -0 sed_i "s/^package .*DTO;$/package ${BASE_OK_PKG}.dto;/"
  find "$SRC_ROOT/com/project/back_end/$DTO_DIR_LOWER" -type f -name "*.java" -print0 \
    | xargs -0 sed_i "s/^package .*dto;$/package ${BASE_OK_PKG}.dto;/"
fi

say "Checking for duplicate AppointmentDTO"
APPTS=($(gr -l "class\\s\\+AppointmentDTO\\b" "$SRC_ROOT" | sort || true))
if [ "${#APPTS[@]}" -gt 1 ]; then
  echo "Found multiple AppointmentDTO files:"
  printf " - %s\n" "${APPTS[@]}"
  echo "Keeping the one in ${BASE_OK_PKG}.dto; others will be moved to *_REMOVED.java"
  keep=""
  for p in "${APPTS[@]}"; do
    if [[ "$p" == *"/com/project/back_end/${DTO_DIR_LOWER}/"* ]]; then keep="$p"; fi
  done
  if [ -n "$keep" ]; then
    for p in "${APPTS[@]}"; do
      if [ "$p" != "$keep" ]; then
        mv "$p" "${p%.java}_REMOVED.java"
        echo " -> moved duplicate: $p"
      fi
    done
  else
    echo "No AppointmentDTO in ${DTO_DIR_LOWER} — please move your canonical DTO there."
  fi
fi

# -------- 4) FILE NAME vs PUBLIC CLASS NAME --------
say "Aligning filenames with public class names"
while IFS= read -r -d '' f; do
  name="$(awk '/public (class|interface|enum) /{for(i=1;i<=NF;i++){if($i=="class"||$i=="interface"||$i=="enum"){print $(i+1); exit}}}' "$f" | sed 's/{//g' | sed 's/<.*//g')"
  if [ -n "${name:-}" ]; then
    base="$(basename "$f" .java)"
    if [ "$name" != "$base" ]; then
      new="$(dirname "$f")/$name.java"
      echo "Renaming file to match public type: $f -> $new"
      if has git; then git mv "$f" "$new" || mv "$f" "$new"; else mv "$f" "$new"; fi
    fi
  fi
done < <(find "$SRC_ROOT" -type f -name "*.java" -print0)

# -------- 5) REPOSITORY PACKAGE CONSISTENCY --------
say "Ensuring repositories live under ${BASE_OK_PKG}.${REPOS_DIR}"
gr -n "interface .*Repository\\b" "$SRC_ROOT" || true
find "$SRC_ROOT" -type f -name "*.java" -print0 | xargs -0 sed_i "s/import ${BASE_OK_PKG}\.[A-Za-z0-9_]*\.repositories\./import ${BASE_OK_PKG}.${REPOS_DIR}./g"

# -------- 6) TOKEN SERVICE FIXES --------
say "Adjusting TokenService imports and overload if needed"
if [ -f "$SRC_ROOT/com/project/back_end/${SERVICES_DIR}/TokenService.java" ]; then
  sed_i "s/import ${BAD_PKG_1}\.repositories\./import ${BASE_OK_PKG}.${REPOS_DIR}./g" "$SRC_ROOT/com/project/back_end/${SERVICES_DIR}/TokenService.java"
  if gr -n "generateToken\\s*\\(\\s*[^,]+\\s*,\\s*[^)]+\\)" "$SRC_ROOT" >/dev/null 2>&1; then
    echo "Detected calls to generateToken(email, id). Ensuring an overload exists..."
    if ! gr -n "String\\s+generateToken\\s*\\(\\s*String\\s+[^,]+\\s*,\\s*Long\\s+[^)]+\\)" "$SRC_ROOT/com/project/back_end/${SERVICES_DIR}/TokenService.java" >/dev/null 2>&1; then
      cat >> "$SRC_ROOT/com/project/back_end/${SERVICES_DIR}/TokenService.java" <<'EOF'

// --- auto-added overload by fix script ---
public String generateToken(String email, Long id) {
    java.util.Date now = new java.util.Date();
    java.util.Date exp = new java.util.Date(now.getTime() + 7L*24*60*60*1000);
    return io.jsonwebtoken.Jwts.builder()
            .setSubject(email)
            .claim("uid", id)
            .setIssuedAt(now)
            .setExpiration(exp)
            .signWith(getSigningKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
            .compact();
}
EOF
      echo "Added TokenService.generateToken(String, Long) overload."
    fi
  fi
fi

# -------- 7) JJWT DEPENDENCIES CHECK --------
say "Checking pom.xml for JJWT 0.11.5"
NEED_ADD=false
if ! gr -n "<artifactId>jjwt-api</artifactId>\\s*<version>0.11.5</version>" "$POM_FILE" >/dev/null 2>&1; then NEED_ADD=true; fi
if ! gr -n "<artifactId>jjwt-impl</artifactId>\\s*<version>0.11.5</version>" "$POM_FILE" >/dev/null 2>&1; then NEED_ADD=true; fi
if ! gr -n "<artifactId>jjwt-jackson</artifactId>\\s*<version>0.11.5</version>" "$POM_FILE" >/dev/null 2>&1; then NEED_ADD=true; fi

if [ "$NEED_ADD" = true ]; then
  say "Adding/ensuring JJWT 0.11.5 deps (simple append inside <dependencies>)"
  if gr -n "<dependencies>" "$POM_FILE" >/dev/null 2>&1; then
    awk '
      /<dependencies>/ && !x { print; print "    <dependency>\n      <groupId>io.jsonwebtoken</groupId>\n      <artifactId>jjwt-api</artifactId>\n      <version>0.11.5</version>\n    </dependency>\n    <dependency>\n      <groupId>io.jsonwebtoken</groupId>\n      <artifactId>jjwt-impl</artifactId>\n      <version>0.11.5</version>\n      <scope>runtime</scope>\n    </dependency>\n    <dependency>\n      <groupId>io.jsonwebtoken</groupId>\n      <artifactId>jjwt-jackson</artifactId>\n      <version>0.11.5</version>\n      <scope>runtime</scope>\n    </dependency>"; x=1; next }1
    ' "$POM_FILE" > "${POM_FILE}.tmp"
    mv "${POM_FILE}.tmp" "$POM_FILE"
  else
    echo "WARNING: <dependencies> not found in pom.xml – add JJWT deps manually."
  fi
else
  echo "JJWT deps look OK."
fi

# -------- 8) REPORT MISSING METHODS (CONTROLLER -> SERVICE) --------
say "Scanning controllers for service calls that may not exist (TODO list)"
gr -n "class .*Controller" "$SRC_ROOT" || true
gr -n "new .*Service" "$SRC_ROOT" || true
echo "TIP: Ensure each called method exists in the Service implementation:"
gr -n "\\.\\w\\+\\(" "$SRC_ROOT/com/project/back_end/controllers" || true

# -------- 9) REPORT MISSING MODELS / GETTERS --------
say "Reporting likely missing models/getters (TODO list)"
gr -n "import ${BASE_OK_PKG}\.${MODELS_DIR}\.Login" "$SRC_ROOT" || true
echo "If Login model is missing, create one under ${BASE_OK_PKG}.${MODELS_DIR} or update imports."
echo "Searching for common missing getters:"
for getter in getFirstName getLastName getPhoneNumber getDateOfBirth getReason getNotes; do
  gr -n "\\b${getter}\\b" "$SRC_ROOT" || true
done

# -------- 10) LocalDateTime vs LocalTime mismatches --------
say "Checking for LocalDateTime/LocalTime mixed usage (TODO list)"
gr -n "LocalDateTime" "$SRC_ROOT" || true
gr -n "LocalTime" "$SRC_ROOT" || true
echo "If a method expects LocalTime but receives LocalDateTime, use .toLocalTime()."

# -------- 11) CLEAN & COMPILE --------
say "Running clean compile (skip tests)"
./mvnw -q -DskipTests clean compile || {
  echo
  echo "Compile still failing. Check the TODOs above; then re-run:"
  echo "  ./mvnw -q -DskipTests clean compile"
  exit 1
}

say "SUCCESS: Compilation passed. You can run:"
echo "./mvnw spring-boot:run"
