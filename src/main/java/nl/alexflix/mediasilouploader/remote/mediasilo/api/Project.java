package nl.alexflix.mediasilouploader.remote.mediasilo.api;

public class Project {
    private String id;
    private int accountId;
    private String name;
    private String description;
    private long dateCreated;
    private long lastActivity;
    private String ownerId;
    private boolean isFavorite;
    private boolean isWatermarked;
    private boolean isDRM;
    private int folderCount;
    private Aggregates aggregates;
    private String poster;
    private String banner;
    private Owner owner;

    @Override
    public String toString() {
        return "Project{" +
                "id='" + id + '\'' +
                ", accountId=" + accountId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", dateCreated=" + dateCreated +
                ", lastActivity=" + lastActivity +
                ", ownerId='" + ownerId + '\'' +
                ", isFavorite=" + isFavorite +
                ", isWatermarked=" + isWatermarked +
                ", isDRM=" + isDRM +
                ", folderCount=" + folderCount +
                ", aggregates=" + aggregates +
                ", poster='" + poster + '\'' +
                ", banner='" + banner + '\'' +
                ", owner=" + owner +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

// Getters and setters for each field
    // Add constructors, toString, and any other necessary methods

    public static class Aggregates {
        private long totalDuration;
        private int totalUsers;
        private int totalAssets;
        private double averageFileSize;
        private int totalFileSize;
        private int totalFolders;
        private double averageDuration;

        // Getters and setters for each field


        @Override
        public String toString() {
            return "Aggregates{" +
                    "totalDuration=" + totalDuration +
                    ", totalUsers=" + totalUsers +
                    ", totalAssets=" + totalAssets +
                    ", averageFileSize=" + averageFileSize +
                    ", totalFileSize=" + totalFileSize +
                    ", totalFolders=" + totalFolders +
                    ", averageDuration=" + averageDuration +
                    '}';
        }
    }

    public static class Owner {
        private String id;
        private String name;
        private String email;

        // Getters and setters for each field


        @Override
        public String toString() {
            return "Owner{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    '}';
        }
    }
}