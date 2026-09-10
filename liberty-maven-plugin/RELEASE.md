# Release du liberty-maven-plugin

## Convention de version

La version du fork suit celle de la derniere version amont integree
(`OpenLiberty/ci.maven`, branche `3.x`). Version courante :
`3.12.3-SNAPSHOT`, alignee sur le tag amont `liberty-maven-3.12.3`.

Les tags du fork sont prefixes `liberty-maven-plugin-` (ex:
`liberty-maven-plugin-3.12.3`), la ou l'amont utilise `liberty-maven-` : les
deux jeux de tags coexistent donc sans collision dans le depot.

## Prerequis

### Credentials Nexus dans `~/.m2/settings.xml`

Ajouter les credentials dans la section `<servers>` :

```xml
<server>
    <id>lutece_releases_repository</id>
    <username>VOTRE_USERNAME</username>
    <password>VOTRE_PASSWORD</password>
</server>
<server>
    <id>lutece_snapshots_repository</id>
    <username>VOTRE_USERNAME</username>
    <password>VOTRE_PASSWORD</password>
</server>
```

## Etape 1 : Prepare

Depuis le repertoire `liberty-maven-plugin/` :

```bash
mvn -Dgpg.skip=true release:prepare
```

Cette commande :
- Modifie la version du pom.xml (retire `-SNAPSHOT`)
- Cree un commit et un tag git (ex: `liberty-maven-plugin-3.12.3`)
- Passe a la version SNAPSHOT suivante (ex: `3.12.4-SNAPSHOT`)
- Cree un fichier `release.properties` (necessaire pour l'etape suivante)

Le fichier `release.properties` est liste dans le `.gitignore` a la racine : il
est normal qu'il n'apparaisse pas dans `git status`. Ne pas le supprimer entre
les etapes 1 et 2, `release:perform` en a besoin.

## Etape 2 : Perform

```bash
mvn release:perform -Darguments="-Dgpg.skip=true -P !sonatype-oss-release"
```

Les options sont importantes :
- `-Dgpg.skip=true` : pas de signature GPG (pas de cle configuree)
- `-P !sonatype-oss-release` : desactive le profil herite du parent OpenLiberty qui tente de publier sur Maven Central via `central-publishing-maven-plugin`. Sans cette option, le deploy echoue car il essaie Sonatype Central au lieu du Nexus Lutece.

Cette commande :
- Checkout le tag depuis GitHub dans `target/checkout/`
- Compile et teste le projet
- Deploie le jar et le pom sur le Nexus Lutece (`https://dev.lutece.paris.fr/nexus/content/repositories/lutece_releases_repository`)

## Depannage

### `release.properties` manquant

Si le fichier `release.properties` a ete supprime apres le `release:prepare` (par un `mvn clean` par exemple), il faut le recreer manuellement dans le repertoire `liberty-maven-plugin/` :

```properties
scm.url=scm\:git\:https\://github.com/lutece-platform/tools-liberty-maven-plugin.git
scm.tag=liberty-maven-plugin-VERSION
scm.commentPrefix=[maven-release-plugin]
exec.additionalArguments=-Dgpg.skip\=true
exec.pomFileName=pom.xml
completedPhase=end-release
projectVersionPolicyId=default
releaseStrategyId=default
project.rel.fr.paris.lutece.tools\:liberty-maven-plugin=VERSION
project.dev.fr.paris.lutece.tools\:liberty-maven-plugin=NEXT_VERSION-SNAPSHOT
project.scm.fr.paris.lutece.tools\:liberty-maven-plugin.connection=scm\:git\:https\://github.com/lutece-platform/tools-liberty-maven-plugin.git
project.scm.fr.paris.lutece.tools\:liberty-maven-plugin.developerConnection=scm\:git\:https\://github.com/lutece-platform/tools-liberty-maven-plugin.git
project.scm.fr.paris.lutece.tools\:liberty-maven-plugin.tag=HEAD
project.scm.fr.paris.lutece.tools\:liberty-maven-plugin.url=https\://github.com/lutece-platform/tools-liberty-maven-plugin.git
```

Remplacer `VERSION` par la version release (ex: `3.12.3`) et `NEXT_VERSION` par la version suivante (ex: `3.12.4`).

### 401 Unauthorized

Les credentials du serveur `lutece_releases_repository` sont absents ou incorrects dans `~/.m2/settings.xml`. Voir la section Prerequis.

### 504 Gateway Timeout

Probleme temporaire du serveur Nexus. Les artefacts ont probablement ete uploades. Verifier sur le Nexus puis relancer si necessaire.

### central-publishing-maven-plugin echoue (server id: maven-central-releases)

Le profil `sonatype-oss-release` est active. Ajouter `-P !sonatype-oss-release` dans les arguments. Ce profil est herite du parent POM OpenLiberty (`io.openliberty.tools:liberty-maven`) et configure la publication vers Maven Central, ce qui n'est pas souhaite pour le deploiement Lutece.
