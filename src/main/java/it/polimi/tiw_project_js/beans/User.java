package it.polimi.tiw_project_js.beans;

public class User {
    public enum Role {
        ADMINISTRATIVE,
        TECHNICAL
    }

    private String username;
    private String firstName;
    private String lastName;
    private String password;
    private String photo;
    private Role role;
    private boolean isManager;
    private boolean isAssignee;

    public User() {}

    public User(String username, String firstName, String lastName,
                String password, String photo, String role,  boolean isManager, boolean isAssignee) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.photo = photo;
        this.role = Role.valueOf(role);
        this.isManager = isManager;
        this.isAssignee = isAssignee;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isAdminStaff() { return role.equals(Role.ADMINISTRATIVE); }
    public boolean isTechnicalStaff() { return role.equals(Role.TECHNICAL); }
    public boolean isManager() {
        return isManager;
    }
    public boolean isAssignee() { return isAssignee; }

    public String getFullName() { return firstName + " " + lastName; }
}

