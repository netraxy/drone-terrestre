# Drone Terrestre 🚗

App Android pour piloter une voiture RC pilotée par Raspberry Pi via WebSocket.

## Utilisation

1. Va dans l'onglet **Actions** de ce repo
2. Attends que le build "Build APK" se termine (5-10 min après le push)
3. Clique sur le build le plus récent → télécharge l'artefact `drone-terrestre-apk`
4. Dézippe, transfère `app-debug.apk` sur ton téléphone
5. Installe (autoriser "Sources inconnues" si demandé)

## Fonctionnement de l'app

- **Écran 1** : scan automatique des réseaux WiFi → tap sur le réseau de la voiture → entre le mot de passe
- Android affiche un dialog de confirmation natif (sécurité), valide
- **Écran 2** : 2 joysticks (direction + vitesse) + bouton ARRÊT
- Envoi WebSocket à 20 Hz vers `ws://192.168.4.1:8765/` (modifiable)

## Côté Raspberry Pi

Voir le fichier `rc_server.py` du chat. Tu dois configurer le Pi en point d'accès
(SSID + mot de passe au choix) et lancer le serveur Python qui écoute sur 8765.
