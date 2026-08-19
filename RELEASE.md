# RELEASE PROCEDURE

set
`RELEASE_VERSION`
`NEXT_VERSION-SNAPSHOT`

```
mvn versions:set -DnewVersion="$RELEASE_VERSION"
mvn versions:commit

git commit -sam "Release $RELEASE_VERSION"
git push
git tag "$RELEASE_VERSION"
git push origin "$RELEASE_VERSION"
```

Trigger the release from https://github.com/crawler-commons/url-frontier/releases
selecting the tag created above

Check that the 2 workflows succeeded (Publish package to the Maven Central Repository) and (Dockerhub Deployment )

Close the [milestone](https://github.com/crawler-commons/url-frontier/milestones) then open a new one. 

Set the next version with:

```
mvn versions:set -DnewVersion="$NEXT_VERSION-SNAPSHOT"
mvn versions:commit
git commit -sam "Post release $RELEASE_VERSION"
git push
```

