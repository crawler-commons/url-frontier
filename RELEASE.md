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
Selecting the tag created above

Check that the 2 workflows succeeded (Publish package to the Maven Central Repository) and (Dockerhub Deployment )

Close the milestone on GH and open a new one

```
mvn versions:set -DnewVersion="$NEXT_VERSION-SNAPSHOT"
mvn versions:commit
git commit -sam "Post release $RELEASE_VERSION"
git push
```

