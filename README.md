# transitive-hull-maven-plugin

A Maven plugin that calculates the transitive hull of all up- and downstream Modules for a given Maven reactor project.

## Demonstration of missing modules in Maven reactor

In `sample-project` run:
```bash
mvn -pl d/pom.xml -am -amd validate
```

It will only output:
```text
...
[INFO] Reactor Build Order:
[INFO]
[INFO] parent                                                             [pom]
[INFO] d                                                                  [jar]
[INFO] e                                                                  [jar]
...
```

Assuming we had changed `d/pom.xml` and would like to build all relevant modules, we would in fact miss module `h` which is required for module `e` to be built.

## Demonstration of transitive hull plugin

Run `mvn install` in the root of this project (to install the plugin to your local Maven repository).

Then, navigate to `sample-project` and run:

```bash
mvn edu.tum.sse:transitive-hull-maven-plugin:0.0.1-SNAPSHOT:transitive-hull -DchangedModules=changedModules.txt
```

which should output:
```text
...
[INFO] --- transitive-hull-maven-plugin:0.0.1-SNAPSHOT:transitive-hull (default-cli) @ parent ---
[INFO] Selected projects:
e
d
parent
h
...
```

## Open questions

- [ ] How can we integrate this with the reactor, to only continue building the identified modules (`session.setProjects()` does not apparently suffice)?
