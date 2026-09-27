# loadtest-app

Petite application Spring Boot "cobaye", sans autre but que de générer chaque
minute une activité CPU + mémoire visible, pour tester **Sentrix Agent**
lorsqu'aucune vraie application n'est encore déployée sur le serveur.

## Ce qu'elle fait

Toutes les 60 secondes (`@Scheduled(fixedRate = 60_000)`) :

1. **Charge CPU** : multiplication réelle de deux matrices 400x400
   (`CpuLoadService`) → pic de CPU visible dans les métriques système.
2. **Charge mémoire** : allocation d'un nouveau bloc de 15 Mo rempli de
   données aléatoires (`MemoryLoadService`) → la RAM utilisée monte
   progressivement.
3. Toutes les 6 minutes (ou au-delà de 12 blocs, ~180 Mo), une partie du
   buffer est libérée → la courbe de RAM redescend, formant un motif en
   "dents de scie" facile à repérer sur un graphe.
4. Un log `INFO` est écrit à chaque étape (démarrage/fin de cycle, durée du
   calcul, état de la heap JVM) dans `/var/log/loadtest-app/app.log`, avec un
   pattern Logback standard.
5. Toutes les 15 minutes (réglable), une **erreur simulée** est générée
   volontairement (exception catchée immédiatement, sans impact) pour tester
   le niveau `ERROR` et le parsing multi-ligne des stack traces côté agent.
   Un `WARN` est aussi émis naturellement quand le seuil mémoire est atteint,
   juste avant la purge.

**Important** : l'erreur simulée est clairement identifiée comme fictive
dans son message (`"ceci est une erreur FICTIVE, generee volontairement..."`)
— ce n'est pas une vraie panne, juste un moyen de vérifier que le dashboard
et le parsing multi-ligne de Sentrix Agent fonctionnent correctement pour
ce niveau de log. Mets `simulate-errors: false` dans `application.yml` pour
la désactiver.

Tout est paramétrable dans `application.yml` (taille des blocs, seuils,
taille de la matrice) sans recompiler.

## Lancer en local

```bash
mvn clean package
java -jar target/loadtest-app.jar
```

Par défaut les logs vont dans `/var/log/loadtest-app/app.log`. Si tu n'as pas
les droits d'écriture sur ce dossier en local, change simplement
`logging.file.name` dans `application.yml` (ex: `./logs/app.log`).

## Lancer avec Docker (stack indépendante de sentrix-agent)

Tout doit vivre sous `/opt/sentrix` sur le serveur. Structure recommandée :

```
/opt/sentrix/
├── sentrix-agent/          ← projet agent existant (docker-compose.yml, sentrix-sources.yml...)
├── loadtest-app/           ← ce projet-ci (dézippe le ici)
└── logs/
    └── loadtest-app/       ← créé une seule fois, sert de point de contact
```

```bash
sudo mkdir -p /opt/sentrix/logs/loadtest-app
sudo chown $USER:$USER /opt/sentrix/logs/loadtest-app   # si besoin
cd /opt/sentrix/loadtest-app
docker compose up -d --build
```

`loadtest-app` et `sentrix-agent` restent deux stacks Docker **séparées**
(deux `docker-compose.yml` distincts, pas de `depends_on`, pas de réseau
Docker commun). Le seul point de contact entre les deux, c'est le dossier
`/opt/sentrix/logs/loadtest-app` sur l'hôte.

- Le `docker-compose.yml` fourni ici monte ce dossier en écriture :
  `/opt/sentrix/logs/loadtest-app:/var/log/loadtest-app`.
- Dans le `docker-compose.yml` de **sentrix-agent**
  (`/opt/sentrix/sentrix-agent/docker-compose.yml`), ajoute le même dossier
  en lecture seule :

```yaml
services:
  sentrix-agent:
    # ... config existante (image, env Keycloak/Kafka, etc.) ...
    volumes:
      - ./sentrix-sources.yml:/config/sentrix-sources.yml:ro
      - /opt/sentrix/logs/loadtest-app:/var/log/loadtest-app:ro   # <-- ajouter cette ligne
```

Les deux conteneurs voient alors le même fichier `app.log`, chacun de son
côté, sans être couplés autrement. Tu peux démarrer/arrêter/redéployer
`loadtest-app` sans toucher à `sentrix-agent`, et inversement — seul le
dossier `/opt/sentrix/logs/loadtest-app` doit continuer d'exister.

**Sur des serveurs différents** : un bind mount ne fonctionne plus, il
faudrait soit monter un partage réseau (NFS/SMB) accessible des deux côtés,
soit faire remonter les logs vers le serveur de l'agent (rsync, agent de
log-shipping, etc.) — dis-le-moi si c'est ton cas, l'approche change.

(Sans Docker du tout : lance `loadtest-app` directement avec
`logging.file.name: /opt/sentrix/logs/loadtest-app/app.log`, et mets ce
même chemin dans `paths:` côté agent — pas de montage nécessaire, l'agent
lit le fichier directement.)

## Déclarer la source de logs dans Sentrix Agent

Dans `/opt/sentrix/sentrix-agent/sentrix-sources.yml` :

```yaml
sentrix:
  agent:
    log-sources:
      - name: loadtest-app
        format: text
        technology: logback
        logger-pattern: "%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %5level --- [%15.15t] %-40.40logger{39} : %msg%n"
        paths:
          - /var/log/loadtest-app/app.log
        multi-line-rule:
          name: java-stacktrace
          pattern: "^\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}"
```

Ce pattern correspond exactement à celui configuré dans
`application.yml` de `loadtest-app` — pas besoin de l'adapter.

Redémarre ensuite l'agent Sentrix pour qu'il prenne en compte la nouvelle
source. Les métriques système (CPU, RAM) qu'il collecte déjà sur le serveur
devraient maintenant afficher des variations toutes les minutes, corrélées
aux logs de `loadtest-app`.

## Ajuster l'intensité de la charge

Dans `application.yml` :

```yaml
loadtest:
  memory-chunk-mb: 15          # Mo alloués par cycle
  memory-max-chunks: 12        # seuil avant purge (~180 Mo)
  release-every-cycles: 6      # purge partielle toutes les N minutes
  cpu-matrix-size: 400         # taille de matrice pour le calcul CPU
  simulate-errors: true        # génère une erreur fictive périodique
  error-every-cycles: 15       # fréquence (en minutes) de cette erreur
```

Augmente `cpu-matrix-size` pour un pic CPU plus marqué, ou
`memory-chunk-mb` / `memory-max-chunks` pour une montée mémoire plus rapide
ou plus haute.
