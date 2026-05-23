package sd2526.trab.impl.zoho;

public class ZohoGetAccount {

    public static void main(String[] args) throws Exception {

        var zoho = Zoho.getInstance();

         System.out.println("Access token: " + zoho.tokenManager.getValidAccessToken());

        var account = Zoho.getInstance().getAccount();
        if (account != null)
            System.out.printf("Account ID: %s, displayName: %s\n", account.accountId(), account.displayName());
        else
            System.err.println("Error...");
    }
}
