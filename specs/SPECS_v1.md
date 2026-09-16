# Spécifications

Les spécifications de notre bibliothèque de communication Java regroupent 3 objets afin de fournir une solution permettant à plusieurs programmes de communiquer entre eux.

## I. BROKER

### I.1. Définition

Un Broker est un objet qui permet de communiquer avec d'autres Brokers ou lui-même. C'est un objet qui centralise des canaux de communications.

### I.2. Fonctionnement

1. Un Broker est identifié par un nom unique dans tout le système.
2. Un Broker possède 2 fonctions :
   - La possibilité d'ouvrir un port correspondant à un entier afin de permettre la communication sur le Broker sur le port. Ouvrir un port revient à construire un Channel. Si le port est déjà ouvert et que le Channel associé est encore disponible, aucune action supplémentaire n'est réalisée. Si le port est déjà ouvert et que le Channel associé n'est plus disponible (qu'il est fermé), alors un nouveau Channel est constitué et remplace le Channel anciennement associé.
   - La possibilité de récupérer le Channel associé au port OUVERT d'un Broker donné. Cette action échoue dans le cas où le Broker cible n'existe pas ou dans le cas où le port n'a pas été ouvert sur le Broker cible, il est recommandé que l'échec marque une différence entre les deux types d'échec. Le Channel retourné n'est pas forcément disponible (ouvert).
     - Un Channel récupéré via cette connection est exclusif, parmi plusieurs Brokers tentant de se connecter au même port sur le même Broker, seul l'un des demandeurs récupèrera l'accès au Channel.

### I.3. Autres

- Les ports d'un Broker sont uniques seulement en interne dans ce Broker. Plusieurs ports portant le même entier dans plusieurs Brokers différents peuvent exister, mais deux ports portant le même entier dans un même Broker ne peuvent exister.

## II. TASK

### II.1. Définition

Une Task est une tâche, un programme démarrant par une fonction et utilisant éventuellement un Broker pour communiquer avec d'autres programmes via d'autres Brokers.

### II.2. Fonctionnement

1. Une Task a connaissance d'un Broker qui lui sert de passerelle de communication.
2. Une Task a une fonction qui décrit la réalisation de la tâche, ou à défaut amorce la réalisation de la tâche.
3. Il doit être possible de récupérer le Broker de la tâche actuellement en cours d'exécution.

### II.3. Autres

- Un Broker n'est pas unique à une Task. Plusieurs Tasks peuvent avoir un même Broker, tout comme plusieurs Tasks peuvent avoir un Broker différent.

## III. CHANNEL

### III.1. Définition

Un Channel est un canal de communication bidirectionnel non-exclusif.

### III.2. Fonctionnement

1. Un Channel ne peut lier qu'au plus deux Brokers, un à chaque extrémité. Avant une connection, un Channel n'est lié qu'à un unique Broker, celui l'ayant créé.
2. Un Channel possède un canal de données séparé pour chacune des deux directions. Un canal de données sur lequel le Broker ouvrant le Channel peut écrire, et sur lequel un Broker connecté peut lire. Et un second canal de données différent sur lequel le Broker connecté peut écrire et sur lequel le Broker ouvrant le Channel peut lire.
3. Un Channel offre la possibilité de lire des données (le canal de données lu dépend du Broker réalisant la lecture). La lecture remplit un tableau d'octets donné, à partir d'un indice donné, le nombre d'octets lu est précisé dans la demande de lecture.
   - La lecture est bloquante tant que le nombre d'octets demandé n'a pas été récupéré.
   - Toute lecture en cours ne doit pas être interrompue par la fermeture du Channel par l'un ou l'autre des Brokers à chaque extrémité.
   - Toute nouvelle lecture est impossible tant qu'une lecture est en cours, dans ce cas, la nouvelle lecture se bloque jusqu'à pouvoir lire (que la précédente lecture soit terminée), elle est désormais en attente.
   - Toute lecture bloquée se termine immédiatement si le Channel est fermé par le Broker à l'autre extrémité. On considère ici que le canal de données en lecture est vide, sinon la lecture se serait débloquée. Les lectures en attente seront donc terminées les unes après les autres puisqu'elles se bloqueront sur un Channel fermé par l'autre extrémité.
   - Toute nouvelle tentative de lecture sur un Channel fermé et vide retourne une erreur.
4. Un Channel offre la possibilité d'écrire des données (le canal de données écrit dépend du Broker réalisant l'écriture). L'écriture écrit les octets présents dans un tableau d'octets donné, à partir d'un indice donné, le nombre d'octets écrit est précisé dans la demande d'écriture.
   - L'écriture est bloquante tant que le nombre d'octets demandé n'a pas été écrit, ou si la fin du tableau est atteinte.
   - Toute écriture en cours ne doit pas être interrompue par la fermeture du Channel par le Broker écrivant.
   - Toute nouvelle écriture est impossible tant qu'une écriture est en cours, dans ce cas, la nouvelle écriture se bloque jusqu'à pouvoir écrire (que la précédente écriture soit terminée), elle est désormais en attente.
   - Toute nouvelle tentative d'écriture sur un Channel fermé par le Broker souhaitant écrire retourne une erreur.
   - Toute écriture en attente doit s'exécuter jusqu'à son terme malgré la fermeture du Channel par le Broker écrivant. Dans le cas où c'est le Broker à l'autre extrémité qui réalise la fermeture, les écritures en attente sont annulés et retournent une erreur.
5. Un Channel doit fournir la possibilité de consulter son état disponible/indisponible (ouvert/fermé ou encore connecté/déconnecté). Cet état indique si le Channel n'est plus utilisable mais ne donne aucune information sur le fait que le Channel soit utilisé ou non (càd lié à deux Brokers).
6. Un Channel doit fournir une méthode de fermeture. La fermeture d'un Channel n'est complète que lorsque la fermeture a été demandé et que les deux canaux de données sont vides. La fermeture d'un Channel doit être appelé avant l'oubli du Channel par l'un ou l'autre des Brokers (déréférencement), dans ce cas, après l'appel le canal de réception (lecture) doit être vidé (pour permettre une fermeture complète).
