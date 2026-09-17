# Spécifications

Les spécifications de notre bibliothèque de communication Java regroupent 3 objets afin de fournir une solution permettant à plusieurs programmes de communiquer entre eux.

## I. BROKER

### I.1. Définition

Un Broker est un objet qui permet de centraliser des canaux de communications.

### I.2. Fonctionnement

1. Un Broker est identifié par un nom unique dans tout le système.
2. Un Broker possède 2 fonctions :
   - La possibilité d'ouvrir un port correspondant à un entier afin de permettre la communication sur le Broker sur le port. Ouvrir un port revient à construire un Channel.
      - Cette action d'ouverture est bloquante tant qu'aucune connection n'est effectuée sur le port.
      - n Tâches peuvent lancer accept sur un même port, l'ordre n'est pas garantie.
   - La possibilité de récupérer le Channel associé au port sur un Broker donné en s'y connectant (action de connection). Le Channel retourné n'est pas forcément disponible (ouvert).
      - Dans le cas où le Broker cible n'existe pas, exception, retourne `null`.
      - Cette action de connection est bloquante jusqu'à ouverture du port demandé.
      - n Tâches peuvent lancer connect sur un même port et un même nom, l'ordre n'est pas garantie.
   > Ces deux fonctions sont un cas de rendez-vous.
3. Un Broker peut être utilisé et connu par plusieurs Tâches.
4. Thread safe induit par la possibilité d'attaque par plusieurs Tâches.

### I.3. Autres

- Les ports d'un Broker sont uniques seulement en interne dans ce Broker. Plusieurs ports portant le même entier dans plusieurs Brokers différents peuvent exister, mais deux ports portant le même entier dans un même Broker ne peuvent exister.

## II. TASK

### II.1. Définition

Une Task est une tâche, un programme démarrant par une fonction et utilisant éventuellement un Broker pour communiquer avec d'autres programmes via d'autres Brokers.

### II.2. Fonctionnement

1. Une Task a connaissance d'un Broker qui lui sert de passerelle de communication.
2. Une Task a une fonction qui décrit la réalisation de la tâche, ou à défaut amorce la réalisation de la tâche.
3. Il doit être possible de récupérer le Broker de la tâche actuellement en cours d'exécution.
4. Une Tâche peut avoir une connection avec plusieurs Channels.

### II.3. Autres

- Un Broker n'est pas unique à une Task. Plusieurs Tasks peuvent avoir un même Broker, tout comme plusieurs Tasks peuvent avoir un Broker différent.

## III. CHANNEL

### III.1. Définition

Un Channel est un canal de communication bidirectionnel non-exclusif utilisant des flux d'octets en full-duplex.

### III.2. Fonctionnement

> Note : On suppose que l'utilisateur teste la déconnexion du Channel à un moment ou à un autre.

1. Un Channel possède deux points de sorties, un à chaque extrémité. Un Channel n'est contenu que dans un unique Broker, celui l'ayant créé. Chaque point de sortie peut être utilisé par plusieurs Tâches, mais le Channel n'appartient à aucune Tâche.
2. Un Channel possède un canal de données (flux d'octets) séparé pour chacune des deux directions (full-duplex). Un canal de données où A peut écrire, et où B peut lire. Et un second canal de données différent sur lequel B peut écrire et sur lequel A peut lire.
   - Un flux d'octets ou canal de données n'a pas de perte et respecte l'ordre FIFO.
3. Un Channel offre la possibilité de lire des données (le canal de données lu dépend du point de sortie réalisant la lecture). La lecture remplit un tableau d'octets donné, à partir d'un indice donné, le nombre d'octets lu est précisé dans la demande de lecture. Renvoie un entier correspondant au nombre d'octets réellement lus, cet entier est compris entre 1 et le nombre d'octets demandé, inclus.
   - La lecture n'est pas synchronisée dans le Channel, si plusieurs Tâches veulent lire par un même point de sortie, il est nécessaire de faire une synchronisation à plus haut niveau. Une seule lecture peut se faire simultanément.
   - La lecture est bloquante quand le flux d'octets est vide et que le point de sortie opposé est ouvert/connecté. La fermeture du point de sortie opposé doit interrompre le blocage des lectures :
      - Si le flux est vide, renvoie 0.
      - Si le flux n'est pas vide, lit normalement.
   - Toute lecture en cours ne doit pas être interrompue par la fermeture du Channel par l'un ou l'autre des points de sortie.
4. Un Channel offre la possibilité d'écrire des données (le canal de données écrit dépend du point de sortie réalisant l'écriture). L'écriture écrit les octets présents dans un tableau d'octets donné, à partir d'un indice donné, le nombre d'octets écrit est précisé dans la demande d'écriture.
   - L'écriture n'est pas synchronisée dans le Channel, si plusieurs Tâches veulent écrire par un même point de sortie, il est nécessaire de faire une synchronisation à plus haut niveau. Une seule écriture peut se faire simultanément.
   - L'écriture est bloquante quand le flux d'octets est plein. La fermeture totale doit interrompre le blocage des écritures.
   - Toute écriture en cours ne doit pas être interrompue par la fermeture du Channel par le point de sortie écrivant. La fermeture du point de sortie opposé n'interrompt pas l'écriture, les octets écrits après cette fermeture seront supprimés.
   - Toute tentative d'écriture sur un Channel fermé par le point de sortie opposé, provoque l'écriture mais les données sont supprimées directement (l'utilisateur n'a donc pas conscience de cette suppression).
5. Un Channel doit fournir la possibilité de consulter l'état disponible/indisponible (ouvert/fermé ou encore connecté/déconnecté) du point de sortie opposé. Cet état indique si le Channel n'est plus utilisable mais ne donne aucune information sur le fait que le Channel soit utilisé ou non (càd lié à deux points de sorties).
6. Un Channel doit fournir une méthode de fermeture. La fermeture d'un Channel n'est complète que lorsque la fermeture a été demandé des deux côtés et que les deux canaux de données sont vides. La fermeture d'un Channel doit être appelé avant l'oubli du Channel par la dernière Tâche connecté à l'un ou l'autre des points de sorties (déréférencement), dans ce cas, après l'appel le canal de réception (lecture) doit être vidé (pour permettre une fermeture complète).
