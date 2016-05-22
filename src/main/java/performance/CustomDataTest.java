package performance;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.account.Accounts;
import com.stormpath.sdk.client.Client;
import com.stormpath.sdk.client.Clients;
import com.stormpath.sdk.directory.CustomData;
import com.stormpath.sdk.lang.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CustomDataTest {

    private static final Logger log = LoggerFactory.getLogger(CustomDataTest.class);

    public static void main(String[] args) {

        String baseUrl = (System.getenv("STORMPATH_BASE_URL") == null) ?
            "https://api.stormpath.com/v1" :
            System.getenv("STORMPATH_BASE_URL");

        String accountHref = System.getenv("STORMPATH_ACCOUNT_HREF");

        Assert.hasText(accountHref, "Provide a fully qualified href to an account using the STORMPATH_ACCOUNT_HREF environment variable.");

        int numIter = Integer.parseInt(args[0]);

        Assert.isTrue(numIter > 0, "Provide the number of iterations to perform as a command line parameter > 0.");

        Client client = Clients.builder().setBaseUrl(baseUrl).build();

        doCustomDataTest(client, accountHref, numIter);
    }

    private static void doCustomDataTest(Client client, String accountHref, int numIter) {
        final Random RNG = new Random();

        int numValid = 0;
        int numFailed = 0;
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (int i = 0; i < numIter; i++) {
            // sleep, so we don't brown out Stormpath
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            // Get account and write custom data.
            Account account = client.getResource(
                accountHref, Account.class, Accounts.options().withCustomData()
            );
            CustomData customData = account.getCustomData();
            customData.put("test-key", RNG.nextInt());

            log.info("Iteration {} of {} updated test-key with: {}", i+1, numIter, customData.get("test-key"));

            // Copy custom data into a map, so we can do equality tests on it later.
            Map<String, Object> customDataMap = new HashMap<>(customData);

            customData.save();

            // Immediately get account back and compare custom data.
            Account returnedAccount = client.getResource(
                accountHref, Account.class, Accounts.options().withCustomData()
            );
            CustomData returnedCustomData = returnedAccount.getCustomData();
            Map<String, Object> returnedCustomDataMap = new HashMap<>(returnedCustomData);

            // Custom Data includes a key "modifiedAt", which is always different. Remove that from the map, so we
            // can validate in earnest.
            customDataMap.remove("modifiedAt");
            returnedCustomDataMap.remove("modifiedAt");

            // Compare and keep stats.
            boolean isValid = Objects.equals(customDataMap, returnedCustomDataMap);
            if (isValid) {
                numValid++;
            } else {
                numFailed++;
                describeFailure(customDataMap, returnedCustomDataMap);
            }
        }

        log.info("Finished {} iterations in {} seconds.", numIter, stopwatch.elapsed(TimeUnit.SECONDS));
        log.info("numValid: {}", numValid);
        log.info("numFailed: {}", numFailed);
    }

    private static void describeFailure(
        Map<String, Object> submittedCustomDataMap,
        Map<String, Object> returnedCustomDataMap
    ) {
        // Union the keys, so we can compare maps on all keys.
        Set<String> allKeySet = Sets.union(submittedCustomDataMap.keySet(), returnedCustomDataMap.keySet());

        // Get the keys in order, so we can describe differences in a stable way.
        List<String> allKeyList = new ArrayList<>(allKeySet);
        Collections.sort(allKeyList);

        // Iterate the keys and log the differences.
        for (String oneKey : allKeyList) {
            Object submittedValue = submittedCustomDataMap.get(oneKey);
            Object returnedValue = returnedCustomDataMap.get(oneKey);
            if (!Objects.equals(submittedValue, returnedValue)) {
                log.error("For key {}, expected {}, got {}.", oneKey, submittedValue, returnedValue);
            }
        }
    }
}
