package performance;

import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.account.Accounts;
import com.stormpath.sdk.application.Application;
import com.stormpath.sdk.client.Client;
import com.stormpath.sdk.client.Clients;
import com.stormpath.sdk.lang.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDataTest {

    private static final Logger log = LoggerFactory.getLogger(CustomDataTest.class);

    public static void main(String[] args) {

        String baseUrl = (System.getenv("STORMPATH_BASE_URL") == null) ?
            "https://api.stormpath.com/v1" :
            System.getenv("STORMPATH_BASE_URL");

        String applicationHref = System.getenv("STORMPATH_APPLICATION_HREF");

        Assert.hasText(applicationHref);

        String email = args[0];

        Assert.hasText(email);

        int numIter = Integer.parseInt(args[1]);

        Client client = Clients.builder().setBaseUrl(baseUrl).build();

        Application application = client.getResource(applicationHref, Application.class);
        log.info("Application: " + application.getHref() + ", " + application.getName());

        doCustomDataTest(application, email, numIter);
    }

    private static final String LOREM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
        "Suspendisse sed ornare nisl, facilisis sodales ante. Nullam et ornare leo, quis congue erat. " +
        "Mauris neque lorem, feugiat nec feugiat in, semper eu purus. Aliquam vel vulputate sapien. " +
        "Maecenas bibendum venenatis nunc viverra convallis. Vivamus sed orci et turpis vulputate condimentum. " +
        "Proin congue est efficitur nunc volutpat, in porttitor tortor auctor. " +
        "Nunc eu libero euismod, laoreet purus ut, lobortis lorem. Nunc nec pulvinar orci. " +
        "Pellentesque dapibus et velit a tincidunt. Morbi ornare purus nec magna imperdiet tincidunt. " +
        "Proin pretium felis nec imperdiet porta. Donec viverra libero dolor, at varius est tempor a. " +
        "Donec nec tortor eu nisi gravida luctus et vitae diam. Fusce at facilisis tellus, et pharetra dui. " +
        "Proin congue, nunc a scelerisque placerat, sapien ipsum consectetur arcu, sit amet convallis dui elit nec augue.";

    private static void doCustomDataTest(Application application, String email, int numIter) {
        numIter = (numIter > 0) ? numIter : 100;

        for (int i=0; i < numIter; i++) {
            Account account = application.getAccounts(Accounts.where(Accounts.email().eqIgnoreCase(email))).single();
            account.getCustomData().put("iter" + i, i + " " + LOREM);
            account.getCustomData().save();

            account = null;
            account = application.getAccounts(Accounts.where(Accounts.email().eqIgnoreCase(email))).single();
            Assert.isTrue((i + " " + LOREM).equals(account.getCustomData().get("iter" + i)));
            log.info("Success! Got iter=" + i + " from CustomData");
        }
    }
}
