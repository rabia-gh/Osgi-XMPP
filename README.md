# Nom broker:XMPP
# Environnement osgi:Eclipse Equinox
## Configuration de broker
-installer openfire:serveur XMPP

-telecharger smack v3.0.3:client XMPP

-importer les .jar: smack.jar ,smacks.jar

-configuration de la connexion entre smack et openfire avant l'implementation des bundles

## Création des bundles
créer le bundle osgi-client-XMPP osgi-client-XMPP>mvn clean install

créer le bundle osgi-EventAdapter osgi-EventAdapter>mvn clean install

créer le bundle osgi-EventAdmin osgi-EventAdmin>mvn clean install

## Exécution des bundles
1)Lancer OSGi: java -jar org.eclipse.equinox.launcher_1.5.500.v20190715-1310.jar -console

2)Exécuter les bundles osgi>install file:c: path\nom_bundle
