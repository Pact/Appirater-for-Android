package com.ijsbrandslob.appirater;

import android.app.Dialog;

public class Config {
	public interface RatingDialogBuilder {
		public Dialog buildRatingDialog();
	}

	/**
	 * If true then Appirater will prompt the user every time the Activity is
	 * started. Useful for testing how your message looks and making sure the
	 * link to your app's review page works.
	 */
	public final boolean debug;

	/**
	 * Users will need to have the same version of your app installed for this
	 * many days before they will be prompted to rate it.
	 */
	public final int daysUntilPrompt;

	/**
	 * An example of a 'use' would be if the user launched the app. Bringing the
	 * app into the foreground (on devices that support it) would also be
	 * considered a 'use'. You tell Appirater about these events using the two
	 * methods: Appirater.appLaunched(); Appirater.appEnteredForeground();
	 * 
	 * Users need to 'use' the same version of the app this many times before
	 * they will be prompted to rate it.
	 */
	public final int usesUntilPrompt;

	/**
	 * A significant event can be anything you want to be in your app. In a
	 * telephone app, a significant event might be placing or receiving a call.
	 * In a game, it might be beating a level or a boss. This is just another
	 * layer of filtering that can be used to make sure that only the most loyal
	 * of your users are being prompted to rate you on the app store. If you
	 * leave this at a value of -1, then this won't be a criteria used for
	 * rating. To tell Appirater that the user has performed a significant
	 * event, call the method: Appirater.userDidSignificantEvent();
	 */
	public final int sigEventsBeforePrompt;

	/**
	 * Once the rating alert is presented to the user, they might select 'Remind
	 * me later'. This value specifies how long (in days) Appirater will wait
	 * before reminding them.
	 */
	public final int timeBeforeReminding;

	/**
	 * A custom dialog builder
	 */
	public final RatingDialogBuilder dialogBuilder;

	private Config(int daysUntilPrompt, int usesUntilPrompt, int timeBeforeReminding, int sigEventsBeforePrompt, RatingDialogBuilder dialogBuilder, boolean debug) {
		this.debug                 = debug;
		this.daysUntilPrompt       = daysUntilPrompt;
		this.timeBeforeReminding   = timeBeforeReminding;
		this.usesUntilPrompt       = usesUntilPrompt;
		this.sigEventsBeforePrompt = sigEventsBeforePrompt;
		this.dialogBuilder         = dialogBuilder;
	}

	public static class Builder {
		public static final int DEFAULT_DAYS_UNTIL_PROMPT       = 15;
		public static final int DEFAULT_USES_UNTIL_PROMPT       = 20;
		public static final int DEFAULT_SIG_EVENTS_UNTIL_PROMPT = -1;
		public static final int DEFAULT_TIME_BEFORE_REMINDING   = 1;

		private int daysUntilPrompt;
		private int usesUntilPrompt;
		private int timeBeforeReminding;
		private int sigEventsBeforePrompt;
		private RatingDialogBuilder dialogBuilder;
		private boolean debug;

		/**
		 * Builds a Builder with default Appirater settings.
		 */
		public Builder() {
			daysUntilPrompt       = DEFAULT_DAYS_UNTIL_PROMPT;
			timeBeforeReminding   = DEFAULT_TIME_BEFORE_REMINDING;
			usesUntilPrompt       = DEFAULT_USES_UNTIL_PROMPT;
			sigEventsBeforePrompt = DEFAULT_SIG_EVENTS_UNTIL_PROMPT;
			dialogBuilder         = null;
		}

		/**
		 * Overrides the default number of days that Appirater will wait before
		 * prompting the user.
		 * 
		 * @param daysUntilPrompt
		 *            The new value.
		 * @return This Builder object.
		 */
		public Builder setDaysUntilPrompt(int daysUntilPrompt) {
			this.daysUntilPrompt = daysUntilPrompt;
			return this;
		}

		/**
		 * Overrides the default number of uses of your app prompting the user.
		 * 
		 * @param usesUntilPrompt
		 *            The new value.
		 * @return This Builder object.
		 */
		public Builder setUsesUntilPrompt(int usesUntilPrompt) {
			this.usesUntilPrompt = usesUntilPrompt;
			return this;
		}

		/**
		 * Overrides the default number of days before reminding the user.
		 * 
		 * @param timeBeforeReminding
		 *            The new value in days.
		 * @return This Builder object.
		 */
		public Builder setTimeBeforeReminding(int timeBeforeReminding) {
			this.timeBeforeReminding = timeBeforeReminding;
			return this;
		}

		/**
		 * Overrides the default number of significant events before prompting
		 * the user.
		 * 
		 * @param sigEventsBeforePrompt
		 *            The new value.
		 * @return This Builder object.
		 */
		public Builder setSigEventsBeforePrompt(int sigEventsBeforePrompt) {
			this.sigEventsBeforePrompt = sigEventsBeforePrompt;
			return this;
		}

		/**
		 * Set a custom dialog builder to create the layout and handle clicks
		 * @param dialogBuilder To build the alert dialog
		 * @return This Builder object
		 */
		public Builder setDialogBuilder(RatingDialogBuilder dialogBuilder) {
			this.dialogBuilder = dialogBuilder;
			return this;
		}

		/**
		 * Enables debugging.
		 * 
		 * @return This Builder object.
		 */
		public Builder enableDebug() {
			this.debug = true;
			return this;
		}

		/**
		 * Builds the Config instance.
		 * 
		 * @return The Config with custom settings.
		 */
		public Config build() {
			return new Config(daysUntilPrompt, usesUntilPrompt,	timeBeforeReminding, sigEventsBeforePrompt, dialogBuilder, debug);
		}
	}
}
