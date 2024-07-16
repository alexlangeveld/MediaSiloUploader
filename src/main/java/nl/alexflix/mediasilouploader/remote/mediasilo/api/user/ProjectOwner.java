package nl.alexflix.mediasilouploader.remote.mediasilo.api.user;
import java.time.LocalDateTime;
public class ProjectOwner {
        String id;
        int numericID;
        String lastName;
        String email;
        ProjectOwnerAdress adress;
        String phone;
        String mobile;
        String company;
        LocalDateTime dateCreated;
        String status;
        String accountID;
        String defaultRoleTemplateID;
        boolean sso;
        String ssoId;
        ProjectOwnerRoles[] roles;
        String[] tags;
        ProjectOwnerPreferences[] preferences;
        long lastLogin;
        boolean MFA;
        String[] projects;
        ShiftProfile shiftProfile;
        String identifier;
        String fullName;
        String secret;
        String value;

}
