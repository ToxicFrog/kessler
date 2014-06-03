ksp-mod-check
=============

Check KSP mods for updates.

Usage:

* `ksp add <link> <name>`

  Add a new mod. Link must be to the mod's KSP forum thread. The script will try to detect the last update date and the latest version.

* `ksp ls`

  List all mods as they are known to the script.

* `ksp rm <id-list>`

  Remove mods from the list.

* `ksp update <id-list>`

  Update mods in the list to the latest version. Equivalent to removing and re-adding them.

* `ksp check`

  Check all mods for newer versions. Up to date mods will appear in green. Outdated mods will appear in red. If a mod is otherwise green but the date appears in yellow, that means you have the latest version but the author hasn't updated it since the latest KSP release.


## License

Copyright © 2014 Ben "ToxicFrog" Kelly, Google Inc.

Distributed under the Apache License v2; see the file COPYING for details.

### Disclaimer

This is not an official Google product and is not supported by Google.
