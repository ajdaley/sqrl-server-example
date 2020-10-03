package com.github.sqrlserverjava.example.data;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * The table where the example app stores userdata. Note this table has nothing SQRL specific in it
 *
 * @author Dave Badia
 * @author Alun Daley
 *
 */
@Entity
@Table(name = "app_user")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long id;

	@Column(nullable = true)
	private String username;

	@Column(nullable = true)
	private String given_Name;

	@Column(nullable = true)
	private String welcome_Phrase;

	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date created_At;

	public AppUser() {
		// Required by JPA
	}

	public AppUser(final String username) {
		this.username = username;
		this.created_At = new Date();
	}

	public AppUser(final String givenName, final String welcomePhrase) {
		this.given_Name = givenName;
		this.welcome_Phrase = welcomePhrase;
		this.created_At = new Date();
	}

	public long getId() {
		return id;
	}

	public String getGiven_Name() {
		return given_Name;
	}

	public String getWelcome_Phrase() {
		return welcome_Phrase;
	}

	public void setGiven_Name(final String givenName) {
		this.given_Name = givenName;
	}

	public void setWelcome_Phrase(final String welcome_Phrase) {
		this.welcome_Phrase = welcome_Phrase;
	}

	public String getUsername() {
		return username;
	}

	public Date getCreated_At() {
		return created_At;
	}

	public void setCreated_At(final Date created_At) {
		this.created_At = created_At;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public void setUsername(final String username) {
		this.username = username;
	}
}

