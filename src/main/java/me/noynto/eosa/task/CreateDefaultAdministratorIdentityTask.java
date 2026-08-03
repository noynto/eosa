package me.noynto.eosa.task;

import me.noynto.eosa.Properties;
import me.noynto.eosa.application.CreateAdministratorIdentity;
import me.noynto.eosa.identity.Identity;

import java.util.logging.Level;
import java.util.logging.Logger;

public record CreateDefaultAdministratorIdentityTask(
        CreateAdministratorIdentity createAdministratorIdentity,
        Properties properties
) {
    private static final String ENABLE = "EOSA_CREATE_DEFAULT_ADMINISTRATOR_IDENTITY_TASK";
    private static final Logger LOGGER = Logger.getLogger(CreateDefaultAdministratorIdentityTask.class.getName());

    public boolean task() {
        LOGGER.log(Level.INFO, "Démarrage de la tâche de création de l'identité administratrice par défaut.");
        CreateAdministratorIdentity.Command command = new CreateAdministratorIdentity.Command(properties.adminName(), properties().adminSecret());
        try {
            Identity identity = createAdministratorIdentity.handle(command);
            LOGGER.log(Level.INFO, "L'identité administratrice est créé avec l'identifiant " + identity.getId() + ".");
            LOGGER.log(Level.INFO, "Fin de la tâche de création de l'identité administratrice par défaut.");
            return true;
        } catch (CreateAdministratorIdentity.AlreadyUsedName e) {
            LOGGER.log(Level.INFO, "L'identité administratrice existe déjà, aucune action nécessaire.");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "L'identité administratrice n'est pas créé.", e);
            return false;
        }
    }

    public static boolean activate() {
        String enable = System.getenv().getOrDefault(ENABLE, "false");
        return Boolean.parseBoolean(enable);
    }
}
