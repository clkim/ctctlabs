package com.ctctlabs.ctctwsjavalib;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.xml.sax.helpers.DefaultHandler;


/*
 * Copyright 1996-2009 Constant Contact, Inc.
 *   Licensed under the Apache License, Version 2.0 (the "License"); 
 *   you may not use this file except in compliance with the License. 
 *   You may obtain a copy of the License at 
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *      
 *   Unless required by applicable law or agreed to in writing, software 
 *   distributed under the License is distributed on an "AS IS" BASIS, 
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *   See the License for the specific language governing permissions and 
 *   limitations under the License.
 */
public class CTCTConnection extends DefaultHandler {
	public static final String API_BASE = "https://api.constantcontact.com";

	private String username;

	private DefaultHttpClient httpclient;

	public enum EventType {
		BOUNCES 	("bounces"), 
		CLICKS 		("clicks"),
		FORWARDS 	("forwards"),
		OPENS		("opens"),
		OPTOUTS		("optouts"),
		SENDS		("sends");
		
		private String name;
		
		EventType(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
	}
	
	public enum CampaignType {
		SENT 		("SENT"),
		SCHEDULED	("SCHEDULED"),
		DRAFT		("DRAFT"),
		RUNNING		("RUNNING"),
		ALL			("ALL");
		
		private String name;
		
		CampaignType(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
	}

	public CTCTConnection() {
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(
		         new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(
		         new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		
		HttpParams params = new BasicHttpParams();
		
		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);

		httpclient = new DefaultHttpClient(cm, params);
	}

	/**
	 * Perform a get request to the web service
	 * @param link URL to perform the get request
	 * @return InputStream to read the response from
	 */
	InputStream doGetRequest(String link) 
	throws ClientProtocolException, IOException {
		HttpGet httpget = new HttpGet(link);
		HttpResponse response = httpclient.execute(httpget);

		int status = response.getStatusLine().getStatusCode();

		// If receive anything but a 200 status, return a null input stream
		if(status == HttpStatus.SC_OK) {
			return response.getEntity().getContent();
		} else {
			return null;
		}
	}
	
	/**
	 * Perform a post request to the web service
	 * @param link URL to perform the get request
	 * @return status code returned by server
	 */
	InputStream doPostRequest(String link, String content) 
	throws ClientProtocolException, IOException {
		HttpPost httppost = new HttpPost(link);
		httppost.addHeader("Content-Type", "application/atom+xml");
		ByteArrayEntity entity = new ByteArrayEntity(content.getBytes());
		httppost.setEntity(entity);
		HttpResponse response = httpclient.execute(httppost);
		
		int status = response.getStatusLine().getStatusCode();

		// If receive anything but a 201 status, return a null input stream
		if(status == HttpStatus.SC_CREATED) {
			return response.getEntity().getContent();
		} else {
			return null;
		}
	}
	
	/**
	 * Perform a put request to the web service
	 * @param link URL to perform the get request
	 * @return status code returned by server
	 */
	InputStream doPutRequest(String link, String content) 
	throws ClientProtocolException, IOException {
		HttpPut httpput = new HttpPut(link);
		httpput.addHeader("Content-Type", "application/atom+xml");
		ByteArrayEntity entity = new ByteArrayEntity(content.getBytes());
		httpput.setEntity(entity);
		HttpResponse response = httpclient.execute(httpput);
		
		int status = response.getStatusLine().getStatusCode();

		// If receive anything but a 200 status, return a null input stream
		// Updates will return a 204, or no content, which is null
		if(status == HttpStatus.SC_OK) {
			return response.getEntity().getContent();
		} else {
			return null;
		}
	}
	
	/**
	 * Perform a delete request to the web service
	 * @param link URL to perform the get request
	 * @return True if status code was successful
	 */
	boolean doDeleteRequest(String link) 
	throws ClientProtocolException, IOException {
		HttpDelete httpDelete = new HttpDelete(link);
		HttpResponse response = httpclient.execute(httpDelete);
		
		return response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT;
	}
	
	

	/**
	 * Validates credentials with the web service and saves authentication information
	 * @param apiKey
	 * @param username
	 * @param password
	 * @return True if the credentials are valid
	 */
	public boolean authenticate(String apiKey, String username, String password) 
	throws ClientProtocolException, IOException {
		String loginUsername = apiKey + "%" + username;
		httpclient.getCredentialsProvider().setCredentials(
				AuthScope.ANY, 
				new UsernamePasswordCredentials(loginUsername, password));
		InputStream stream = doGetRequest(API_BASE + "/ws/customers/" + username + "/");
		if(stream != null) {
			this.username = username;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Gets a ContactListIterator to retrieve all contact lists associated with the authenticated user
	 */
	public ContactListIterator getContactLists() 
	throws InvalidCredentialsException, ClientProtocolException, IOException {
		if(username == null) {
			throw new InvalidCredentialsException();
		}

		String link = API_BASE + "/ws/customers/" + username + "/lists";
		ContactListIterator iterator = new ContactListIterator(this, link);
		iterator.loadNextPage();

		return iterator;
	}

	/**
	 * Gets a single ContactList based on its "link" attribute
	 */
	public ContactList getContactList(String link)
	throws InvalidCredentialsException, ClientProtocolException, IOException {
		if(username == null) {
			throw new InvalidCredentialsException();
		}
		
		HashMap<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("Link", link);
		ContactList contactList = new ContactList(attributes, this, false);
		
		// Get an attribute, this should force the ModelObject to automatically populate
		if(contactList.getAttribute("ContactListId") == null || 
				contactList.getAttribute("ContactListId").equals("")) {
			return null;
		}

		return contactList;
	}

	/**
	 * Gets a ContactListIterator to retrieve all contact lists associated with the contact list
	 * @param link Link attribute of the contact list
	 */
	public ContactIterator getContactListMembers(String link) 
	throws InvalidCredentialsException, ClientProtocolException, IOException {
		if(username == null) {
			throw new InvalidCredentialsException();
		}

		ContactIterator iterator = new ContactIterator(this, API_BASE + link + "/members");
		iterator.loadNextPage();

		return iterator;
	}

	/**
	 * Gets a ContactIterator to retrieve all contacts associated with the authenticated user
	 * Stored in reverse chronological order
	 */
	public ContactIterator getContacts() 
	throws InvalidCredentialsException, ClientProtocolException, IOException {
		if(username == null) {
			throw new InvalidCredentialsException();
		}

		String link = API_BASE + "/ws/customers/" + username + "/contacts";
		ContactIterator iterator = new ContactIterator(this, link);
		iterator.loadNextPage();

		return iterator;
	}

	/**
	 * Gets a single Contact based on its EmailAddress attribute
	 * Does not populate as much information as getContactById
	 */
	public Contact getContactByEmail(String email) 
	throws InvalidCredentialsException, ClientProtocolException, IOException {
		if(username == null) {
			throw new InvalidCredentialsException();
		}

		HashMap<String, Object> attributes = new HashMap<String, Object>();
		String link = "/ws/customers/" + username + "/contacts?email=" + email;
		attributes.put("Link", link);
		Contact contact = new Contact(attributes, this, false);
		
		// Get an attribute, this should force the ModelObject to automatically populate
		if(contact.getAttribute("ContactId") == null || 
				contact.getAttribute("ContactId").equals("http://api.constantcontact.com/ws/customers/" + username + "/contacts")) {
			return null;
		}

		return contact;
	}

	/**
	 * Gets a single Contact based on its Link attribute
	 */
	public Contact getContactByLink(String link) 
	throws InvalidCredentialsException, ClientProtocolException, IOException {
		if(username == null) {
			throw new InvalidCredentialsException();
		}

		HashMap<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("Link", link);
		Contact contact = new Contact(attributes, this, false);
		
		// Get an attribute, this should force the ModelObject to automatically populate
		if(contact.getAttribute("ContactId") == null || 
				contact.getAttribute("ContactId").equals("")) {
			return null;
		}

		return contact;
	}

	/**
	 * Gets a CampaignIterator to retrieve all campaigns associated with the authenticated user
	 * Stored in reverse chronological order
	 */
	public CampaignIterator getCampaigns(CampaignType type) 
	throws InvalidCredentialsException, ClientProtocolException, IOException {
		if(username == null) {
			throw new InvalidCredentialsException();
		}
		
		String link = API_BASE + "/ws/customers/" + username + "/campaigns";
		
		if(type != CampaignType.ALL) {
			link += "?status=" + type.getName();
		}
		
		CampaignIterator iterator = new CampaignIterator(this, link);
		iterator.loadNextPage();
		
		return iterator;
	}

	/**
	 * Gets a single Campaign based on its Link attribute
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws InvalidCredentialsException 
	 */
	public Campaign getCampaign(String link) 
	throws ClientProtocolException, IOException, InvalidCredentialsException {
		if(username == null) {
			throw new InvalidCredentialsException();
		}
		
		HashMap<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("Link", link);
		Campaign campaign = new Campaign(attributes, this);
		
		// Get an attribute, this should force the ModelObject to automatically populate
		if(campaign.getAttribute("CampaignId") == null || 
				campaign.getAttribute("CampaignId").equals("")) {
			return null;
		}

		return campaign;
	}

	/**
	 * Returns a ContactEventIterator to retrieve all ContactEvents associated with a Contact
	 * @param contactLink Link attribute of contact
	 * @param type The type of event to retrieve
	 */
	public ContactEventIterator getContactEvents(String contactLink, EventType type) 
	throws InvalidCredentialsException, ClientProtocolException, IOException {
		if(username == null) {
			throw new InvalidCredentialsException();
		}

		ContactEventIterator iterator = new ContactEventIterator(this, 
				API_BASE + contactLink + "/events/" + type.getName());
		iterator.loadNextPage();

		return iterator;
	}

	/**
	 * Returns a ContactEventIterator to retrieve all ContactEvents associated with a Campaign
	 * @param campaignLink Link attribute of campaign
	 * @param type The type of event to retrieve
	 * @return A list of CampaignEventIterators. Only one for all event types except clicks
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<CampaignEventIterator> getCampaignEvents(String campaignLink, EventType type) 
	throws InvalidCredentialsException, ClientProtocolException, IOException {
		if(username == null) {
			throw new InvalidCredentialsException();
		}

		ArrayList<CampaignEventIterator> iterators = new ArrayList<CampaignEventIterator>();
		if(type == EventType.CLICKS) {
			Campaign campaign = getCampaign(campaignLink);
			if(campaign.hasAttribute("Urls")) {
				for(String eventUrl : (ArrayList<String>)campaign.getAttribute("Urls")) {
					CampaignEventIterator iterator = new CampaignEventIterator(this, 
							eventUrl.replace("http://api.constantcontact.com", "https://api.constantcontact.com"));
					iterator.loadNextPage();
					iterators.add(iterator);
				}
			}
			
			return iterators;
		} else {
			CampaignEventIterator iterator = new CampaignEventIterator(this, 
					API_BASE + campaignLink + "/events/" + type.getName());
			iterator.loadNextPage();
			
			iterators.add(iterator);
			return iterators;
		}
	}

	/**
	 * Creates a ContactList with the HashMap of attributes
	 * @return The ContactList created
	 */
	public ContactList createContactList(HashMap<String, Object> attributes) 
	throws InvalidCredentialsException, ClientProtocolException, IOException {
		ContactList contactList = new ContactList(attributes, this, true);
		contactList.setAttribute("Link", "/ws/customers/" + username + "/lists");
		return contactList;
	}

	/**
	 * Creates a Contact with the HashMap of attributes
	 * @return The Contact created
	 */
	public Contact createContact(HashMap<String, Object> attributes) 
	throws InvalidCredentialsException, ClientProtocolException, IOException {
		Contact contact = new Contact(attributes, this, true);
		contact.setAttribute("Link", "/ws/customers/" + username + "/contacts");
		return contact;
	}
	
	/**
	 * Creates a Schedule with the HashMap of attributes for the campaign and at the local time specified [clk] 
	 * @param attributes  is a map of key-value pairs pertaining to the model object
	 * @param campaignId  is the numeric id representing the campaign to be scheduled
	 * @param scheduledTime  is when the email is to be sent; will be 16 minutes from now if sooner than that or if null
	 * @return The Schedule created
	 * @author CL Kim
	 */
	public Schedule createSchedule(HashMap<String, Object> attributes, String campaignId, Date scheduledTime)
			throws InvalidCredentialsException, ClientProtocolException, IOException {
		Schedule schedule = new Schedule(attributes, this, true);
		schedule.setAttribute("Link", "/ws/customers/" + username + "/campaigns/"+campaignId +"/schedules");

		// get time now
		Calendar cTime = Calendar.getInstance();
		// an email seems must be scheduled 15 minutes in future; adding 1 more minute just for possible server time differences
		cTime.add(Calendar.MINUTE, 16);
		// get soonest time to schedule email campaign as a Date
		Date soonestAllowed = cTime.getTime();
		// check whether scheduled time passed in is null or is sooner than allowed
		if (scheduledTime == null ||
				scheduledTime.getTime() < soonestAllowed.getTime()) {
			scheduledTime = soonestAllowed;
		}
		// set date pattern
		String format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
		SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		// finally, set ScheduledTime attribute value in format required
		schedule.setAttribute("ScheduledTime", sdf.format(scheduledTime));
		return schedule;
	}
	
	/**
	 * Creates an email Campaign with HashMap of attributes and the data for the campaign in the parameters
	 * @param attributes
	 * @param campaignName
	 * @param emailSubject
	 * @param listId
	 * @param emailContent
	 * @param emailTextContent
	 * @param fromName
	 * @param senderEmailId
	 * @param senderEmailAddress
	 * @return The Campaign created
	 * @author CL Kim
	 */
	public Campaign createCampaign(HashMap<String, Object> attributes, String campaignName, String emailSubject,
								   String listId, String emailContent, String emailTextContent,
								   String fromName, String senderEmailId, String senderEmailAddress) {
		Campaign campaign = new Campaign(attributes, this, true);
		campaign.setAttribute("Link", "/ws/customers/" + username + "/campaigns");
		// set email campaign name, subject
		campaign.setAttribute("Name", campaignName);
		campaign.setAttribute("Subject", emailSubject);
		// set contact list to receive the campaign
		campaign.setAttribute("ContactListPath", "/ws/customers/" + username + "/lists/" + listId);
		// set html content of email to be sent
		campaign.setAttribute("EmailContent", emailContent);
		// set text content of email to be sent
		campaign.setAttribute("EmailTextContent", emailTextContent);
		// set sender info needed by api
		campaign.setAttribute("FromName", fromName);
		campaign.setAttribute("SenderEmailAddressPath", "/ws/customers/" + username + "/settings/emailaddresses/" + senderEmailId);
		campaign.setAttribute("SenderEmailAddress", senderEmailAddress);
		return campaign;
	}
}
