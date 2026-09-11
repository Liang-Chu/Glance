# Glance keeps no rules of its own.
#
# WorkManager, DataStore and Compose each ship their own consumer rules, and this
# app reflects on nothing: the worker is named by the request that enqueues it,
# and the only parsing is org.json against fields read by literal name.
#
# If something breaks only in a release build, the cause is almost certainly a
# library rule that is missing rather than anything here — add the keep rule for
# that library, not a blanket one.
