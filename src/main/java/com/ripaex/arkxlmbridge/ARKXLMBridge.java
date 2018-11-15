package com.ripaex.arkxlmbridge;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.FormatException;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Memo;
import org.stellar.sdk.Network;
import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.Server;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.requests.ErrorResponse;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

@SuppressWarnings({ "unused", "resource" })
public class ARKXLMBridge {

	private static final Logger LOG = Logger.getLogger(ARKXLMBridge.class.getName());

	public static void main(String[] args) {

		boolean cycle = true;
		Scanner scan = new Scanner(System.in);
		while (cycle) {
			LOG.info("0. Exit");
			LOG.info("1. Account Info");
			LOG.info("2. Send");
			LOG.info("3. Start a Receiver Listener");
			LOG.info("4. Create Random Keys");
			LOG.info("5. Create Account");
			LOG.info("Make your choice: ");
			String choice = scan.nextLine();
			if ("0".equals(choice)) {
				cycle = false;
				break;
			} else if ("1".equals(choice)) {
				try {
					LOG.info("Insert Account (PUBLIC): ");
					String account = scan.nextLine();
					KeyPair accountKP = KeyPair.fromAccountId(account);
					getAccountInfo(Constants.TESTNET_ENDPOINT, accountKP);
				} catch (IOException | FormatException | ErrorResponse e) {
					LOG.error(e.getMessage(), e);
				}
			} else if ("2".equals(choice)) {
				try {
					LOG.info("Insert Source (SECRET): ");
					String source = scan.nextLine();
					KeyPair sourceKP = KeyPair.fromSecretSeed(source);
					LOG.info("Insert Destination (PUBLIC): ");
					String destination = scan.nextLine();
					KeyPair destinationKP = KeyPair.fromAccountId(destination);

					LOG.info("Insert Amount: ");
					String amount = scan.nextLine();
					if (NumberUtils.isCreatable(amount)) {
						send(Constants.TESTNET_ENDPOINT, sourceKP, destinationKP, amount);
					} else {
						LOG.error("Not a number: " + amount);
					}
				} catch (IOException | FormatException e) {
					LOG.error(e.getMessage(), e);
				}
			} else if ("3".equals(choice)) {
				try {
					LOG.info("Insert Account (PUBLIC): ");
					String account = scan.nextLine();
					Receiver receiver = new Receiver(account);
				} catch (FormatException e) {
					LOG.error(e.getMessage(), e);
				}
			} else if ("4".equals(choice)) {
				createRandomKeys();
			} else if ("5".equals(choice)) {
				try {
					LOG.info("Insert Account (PUBLIC): ");
					String account = scan.nextLine();
					KeyPair accountKP = KeyPair.fromAccountId(account);
					createTestNETAccount(accountKP);
				} catch (IOException | FormatException e) {
					LOG.error(e.getMessage(), e);
				}
			}

		}
		scan.close();
		LOG.info("Application Ended");
	}

	private static void send(String endpoint, KeyPair source, KeyPair destination, String amount) throws IOException {

		Network.useTestNetwork();
		Server server = new Server(endpoint);

		// First, check to make sure that the destination account exists.
		// You could skip this, but if the account does not exist, you will be charged
		// the transaction fee when the transaction fails.
		// It will throw HttpResponseException if account does not exist or there was
		// another error.
		server.accounts().account(destination);

		// If there was no error, load up-to-date information on your account.
		AccountResponse sourceAccount = server.accounts().account(source);

		// Start building the transaction.
		Transaction transaction = new Transaction.Builder(sourceAccount)
				.addOperation(new PaymentOperation.Builder(destination, new AssetTypeNative(), amount).build())
				// A memo allows you to add your own metadata to a transaction. It's
				// optional and does not affect how Stellar treats the transaction.
				.addMemo(Memo.text("Test Transaction")).build();
		// Sign the transaction to prove you are actually the person sending it.
		transaction.sign(source);

		// And finally, send it off to Stellar!
		SubmitTransactionResponse response = server.submitTransaction(transaction);
		LOG.info("Success!");
	}

	private static void getAccountInfo(String endpoint, KeyPair pair) throws IOException {

		Server server = new Server(endpoint);
		AccountResponse account;
		account = server.accounts().account(pair);
		LOG.debug("Balances for account " + pair.getAccountId());
		for (AccountResponse.Balance balance : account.getBalances()) {
			LOG.info(String.format("Type: %s, Code: %s, Balance: %s", balance.getAssetType(), balance.getAssetCode(),
					balance.getBalance()));
		}
	}

	private static void createTestNETAccount(KeyPair pair) throws MalformedURLException, IOException {

		String friendbotUrl = String.format("https://friendbot.stellar.org/?addr=%s", pair.getAccountId());
		InputStream response;
		response = new URL(friendbotUrl).openStream();
		String body = new Scanner(response, "UTF-8").useDelimiter("\\A").next();
		LOG.info("SUCCESS! You have a new account :)");
		LOG.debug(body);
	}

	private static void createRandomKeys() {
		KeyPair pair = KeyPair.random();
		LOG.info("Account Secret: " + new String(pair.getSecretSeed()));
		LOG.info("Account ID: " + pair.getAccountId());
	}
}
