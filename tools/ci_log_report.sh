#!/usr/bin/env bash
# CI helper: prints the lines of a Minecraft run log that matter (warnings, errors, stack causes, anything from
# this mod) plus loading milestones, without dumping the whole log.
log="${1:?log file}"
[[ -f "$log" ]] || { echo "no $log"; exit 0; }
echo "---- milestones ----"
grep -nE 'Done \(|All [0-9]+ required tests|Running [0-9]+ tests|tests? (passed|failed)|Loaded [0-9]+ (recipes|advancements)|Reloading ResourceManager|Created: .*atlas|Sound engine started|Stopping server|BUILD (SUCCESSFUL|FAILED)' "$log" | head -40
echo "---- problems ----"
grep -nE '/(WARN|ERROR|FATAL)\]|Exception|Caused by|Missing|Unable to|Failed to|frieren' "$log" \
  | grep -vE 'DEBUG|Ignoring|Reflections' | head -400
exit 0
