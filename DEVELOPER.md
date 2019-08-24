# OpenTrack: Developer information

## Create releases for FDroid

1. Get next _version code_ (master branch): `git rev-list HEAD --count master`
2. Decide on _version name_ (semantic versioning)
3. Manually update _version code_ and _version name_ in `AndroidManifest.xml` (used by FDroid)
4. Create changelog (`_version code_.txt`)
5. Create commit with all changes
6. Push commits
7. Tag newly create commit with _version name_
8. Push the tagged to public repository  
