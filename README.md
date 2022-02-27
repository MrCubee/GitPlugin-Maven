# Git Plugin

Allows you to retrieve some information from your git repository.

## How to use it ?

### Maven:

#### repository:
```xml
<repositories>
    <repository>
        <id>mrcubee-minecraft</id>
        <url>http://nexus.mrcubee.net/repository/minecraft/</url>
    </repository>
</repositories>
```

#### build plugin:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>fr.mrcubee.maven</groupId>
            <artifactId>gitplugin</artifactId>
            <version>1.0</version>
            <executions>
                <execution>
                    <phase>initialize</phase>
                    <goals>
                        <goal>parse</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

#### Properties:
```
${git.branch.name} | Get the short name of the current branch that HEAD points to.
${git.branch.name_full} | Get the name of the reference that HEAD points to.
${git.branch.authors} | Get authors of the current branch that HEAD points to (like: Author1, Author2, Author3).
${git.commit.last.sha1} | Get string form of the SHA-1, in lower case hexadecimal from last commit.
${git.commit.last.sha1_short} | Get string form of the short SHA-1, in lower case hexadecimal from last commit.
${git.commit.last.author} | Get author of last commit in current branch that HEAD points to.
```

#### Example Custom Properties:
```xml
<properties>
    <commit.version>${git.branch.name}-${git.commit.last.sha1_short}</commit.version>
    <plugin.version>${project.version} (git ${commit.version})</plugin.version>
</properties>
```

#### Example bukkit's plugin.yml
```yaml
name: ${project.name}
version: ${plugin.version}
authors: [${git.branch.authors}]
website: ${project.url}
main: plugin.main
```