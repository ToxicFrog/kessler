ksp-mod-check
=============

Check KSP mods for updates.

The basic idea is that you `ksp add` mods as you install them. When you're ready to do a round of mod updates, you `ksp refresh` to make it aware of the latest mod versions, then `ksp check` to see which ones you need to update. You then `ksp updated` the ones you actually updated, and `ksp add` or `ksp rm` ones that you`ve added or removed.

Commands:

* `ksp add <link> <name>`

  Add a new mod. Link must be to the mod's KSP forum thread. The script will try to detect the last update date and the latest version.

* `ksp ls`

  List all mods as they are known to the script.

* `ksp rm <id-list>`

  Remove mods from the list.

* `ksp refresh`

  Download the latest version of the forum threads.

* `ksp updated <id-list>`

  Mark the listed mods as being updated to the latest version.

* `ksp check`

  Check mods against the local cache and report any that need to be updated, along with a link to their forum thread.

* `ksp url <id-list>`

  Display the url to a given mod's forum thread.


## License

Copyright © 2014 Ben "ToxicFrog" Kelly, Google Inc.

Distributed under the Apache License v2; see the file COPYING for details.

### Disclaimer

This is not an official Google product and is not supported by Google.
