import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class HashCrackerPW {
    public static class UserDetails {
        private String username;
        private String salt;
        private String encodedHash;
        private String password = null;

        public UserDetails(String username, String salt, String encodedHash) {
            this.username = username;
            this.salt = salt;
            this.encodedHash = encodedHash;
        }

        public UserDetails(String username, String salt, String encodedHash, String password) {
            this(username, salt, encodedHash);
            this.password = password;
        }

        @Override
        public String toString() {
            StringJoiner sj = new StringJoiner("\n\t");
            sj.add(getUsername())
              .add("Salt: " + getSalt())
              .add("Encoded Hash: " + getEncodedHash())
              .add("Password: " + getPassword());
            return sj.toString();
        }

        public String getUsername()                    {return username;}

        public void setUsername(String username)       {this.username = username;}

        public String getSalt()                        {return salt;}

        public void setSalt(String salt)               {this.salt = salt;}

        public String getEncodedHash()                 {return encodedHash;}

        public void setEncodedHash(String encodedHash) {this.encodedHash = encodedHash;}

        public String getPassword()                    {return password;}

        public void setPassword(String password)       {this.password = password;}
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> userPasswords = crackHashes(new File("password.txt"), new File("2B_stripped.txt"));
//        Map<String, String> userPasswords = crackHashes(new File("password.txt"), new File("password_test.txt"));

        userPasswords.put("randall_munroe", "CorrectHorseBatteryStaple"); // Inferred

        File cracked = new File("cracked_passwords_2B.txt");
        FileOutputStream crackedOut = new FileOutputStream(cracked);
        userPasswords.forEach((u, p) -> {
            try {
                String lineOut = u + ":" + p + "\n";
                crackedOut.write(lineOut.getBytes(StandardCharsets.UTF_8));

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    public static Map<String, String> crackHashes(File userSaltHash, File passwordDictionary) {
        HashMap<String, UserDetails> users = new HashMap<>();
        ConcurrentHashMap<String, String> crackedPasswords = new ConcurrentHashMap<>();

        getParallelLineStream(userSaltHash).forEach(v -> {
            String[] parts = v.split(":");
            String username = parts[0];
            String salt = parts[1];
            String encodedHash = parts[2];

            users.put(username, new UserDetails(username, salt, encodedHash));
        });

        getParallelLineStream(passwordDictionary).forEach(l ->
                        users.entrySet()
                             .parallelStream()
                             .map(Map.Entry::getValue)
                             .filter(u -> u.getPassword() == null)
                             .filter(u -> {
                                 String lineSaltHash;
                                 MessageDigest digest;
                                 try {
                                     digest = MessageDigest.getInstance("SHA-256");
                                 } catch (NoSuchAlgorithmException e) {
                                     e.printStackTrace();
                                     return false;
                                 }

                                 lineSaltHash = Base64.getEncoder()
                                                      .encodeToString(digest.digest((u.getSalt() + l).getBytes(StandardCharsets.UTF_8)));
                                 boolean isMatch = lineSaltHash.equals(u.getEncodedHash());

                                 if (isMatch)
                                     u.setPassword(l);

                                 return isMatch;
                             })
                             .forEach(u -> {
                                 System.out.printf("FOUND: %s\n\t\t- %s\n", u.getUsername(), u.getPassword());
                                 crackedPasswords.put(u.getUsername(), u.getPassword());
                             })
        );

        return crackedPasswords;
    }

    public static Stream<String> getParallelLineStream(File file) {
        Stream<String> lines = null;
        try {
            lines = Files.lines(file.toPath())
                         .parallel();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    public static void searchPasswordsForFunsies(File file, String substr) {
        Stream<String> lines = getParallelLineStream(file);

        lines.filter(v -> v.toLowerCase(Locale.ROOT)
                           .contains(substr))
             .forEach(System.out::println);
    }
}
