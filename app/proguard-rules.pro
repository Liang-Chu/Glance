# Preserve source positions so exported crash stacks can be retraced with this build's mapping.txt.
-keepattributes SourceFile,LineNumberTable
#
# WorkManager, DataStore and Compose each ship their own consumer rules, and this
# app reflects on nothing: the worker is named by the request that enqueues it,
# and the only parsing is org.json against fields read by literal name.
#
# Archive mapping.txt alongside each distributed APK. Do not add blanket keep rules.
