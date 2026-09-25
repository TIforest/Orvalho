import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

/** Execute com Java 26. A chave privada fica fora do projeto e nunca é publicada. */
public class AssinarVersao {
    public static void main(String[] args) throws Exception {
        Path dir = Path.of(System.getProperty("user.home"), ".orvalho-publicacao");
        Files.createDirectories(dir);
        Path secret = dir.resolve("ed25519.pk8");
        Path pub = dir.resolve("ed25519.pub");
        if (args[0].equals("init")) {
            if (!Files.exists(secret)) {
                KeyPair keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
                Files.write(secret, keys.getPrivate().getEncoded(), StandardOpenOption.CREATE_NEW);
                Files.writeString(pub, Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()), StandardOpenOption.CREATE_NEW);
            }
            System.out.println(Files.readString(pub)); return;
        }
        if (!args[0].equals("sign") || args.length != 5) throw new IllegalArgumentException("sign versao url instalador saida");
        Path installer = Path.of(args[3]);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var in = Files.newInputStream(installer)) {
            byte[] buf = new byte[65536]; int n;
            while ((n = in.read(buf)) != -1) digest.update(buf, 0, n);
        }
        byte[] payload = ("version=" + args[1] + "\nurl=" + args[2] + "\nsha256="
                + HexFormat.of().formatHex(digest.digest()) + "\nsize=" + Files.size(installer) + "\n").getBytes(StandardCharsets.UTF_8);
        PrivateKey key = KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(secret)));
        Signature sign = Signature.getInstance("Ed25519"); sign.initSign(key); sign.update(payload);
        Files.writeString(Path.of(args[4]), "payload=" + Base64.getEncoder().encodeToString(payload)
                + "\nsignature=" + Base64.getEncoder().encodeToString(sign.sign()) + "\n");
        System.out.println("Manifesto assinado: " + args[4]);
    }
}
