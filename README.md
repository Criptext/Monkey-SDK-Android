# Getting Started

MonkeyKit on Android supports 4.1 (API 16) and above.

## Using Gradle + Maven

In your app's build.gradle file add the following block to your repositories block:
```
repositories { 
    maven {
        url 'https://dl.bintray.com/criptext/maven'
    } 
}
```

Then add the following to your app's build.gradle file dependencies block:
```
dependencies {
    compile ('com.criptext:MonkeyKit:1.1.0@aar') {
        transitive = true;
    }
}
```

## Extending MonkeyKit service

To use MonkeyKit service, you need to extend `MonkeyKit.java` in your application and implement all of its abstract methods. You must create a database that MonkeyKit can access to store messages that are received while your activites are closed. The following methods must be implemented:

- `public abstract void storeMessage(MOKMessage message);`
  
This method stores a message received by MonkeyKit into your database. You should store them in a way that is convenient for your application to access, since MonkeyKit does not query this messages, only stores them. This is specially useful for messages that are received while your activites are closed. The implementation should be asynchronous to maximize your apps performance.

- `public abstract void storeMessageBatch(ArrayList<MOKMessage> messages);`
  
This method stores a group of messages received by MonkeyKit into your database. Whenever MonkeyKit reconnects, it catches up with the server and receives all the messages that should have been received during the down time. You should store them in a way that is convenient for your application to access, since MonkeyKit does not query this messages, only stores them. Since this method could receive a list of potentially hundreds of messages, the implementation should be asynchronous.

  
## Register user with MonkeyKit

In order to send and receive messages a user must first register and get a session id. This process should be done only once: the first time the user opens your app. To do this, first, you must create a new `MonkeyInit` object

```
MonkeyInit init = new MonkeyInit(MyActivity.this, null, MY_APP_ID, MY_APP_KEY){
    @Override
    public void onSessionOK(String sessionID){
        persistSession(sessionID);
    }
}.register();
```

The constructor recieves 4 arguments: 
- A context reference 
- A string with a previous session ID
- A string with the app's ID
- A string with the app's secret key

If you have determined that the user already has a previous session and would like to keep using it, don't use it immediately. First register with that session ID as the second argument so that MonkeyKit can verify that it is a valid ID and can still be used. For new users you can use `null` or an empty string as the second argument.

As for the last two arguments, You should have received an app ID and secret key from Criptext. 

Finally override the `onSessionOK` method which is a callback containing a single argument: A valid session ID. The register process consists of various HTTP requests so it is executed asynchronously. After it's all done, the callback with your new session ID is executed. Once you have persisted it, you can start your MonkeyKit service and comence sending and receiving messages.

After you have yout `MonkeyInit` object ready, you can start the register
process by calling the `register()` method.

## Starting the service

Once you have your session ID ready, you can start the MonkeyKit service using the static method `startMonkeyService`. The arguments for this method are: 
- A context reference
- The class that extends MonkeyKit implementing all the abstract methods 
- A string with the user's full name
- A string with the app's ID
- A string with the app's secret key
```
MonkeyKit.startMonkeyService(MyActivity.this, MyMonkeyService.class,
				user.getFullName(),
				user.getSessionID(),
				MY_APP_ID, MY_APP_KEY);

```
After starting the service MonkeyKit will connect to the server and immediately start receiving messages.

## Adding a delegate

MonkeyKit needs a delegate to tell your application about the messages and notifications that it has received. Any object that you wish to use as delegate should implement the `MonkeyKitDelegate` interface. This interface has lots of methods, but they all basically callbacks that return `void`. You can use an `Activity` as a delegate like this:

```
public class MyActivity extends Activity implements MonkeyKitDelegate {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_activity);
        MonkeyKit.instance().addDelegate(this);
    }

    @Override
    public void onMessageReceived(MOKMessage message){
        Toast.makeText(this, "Received " + message.getMsg(), Toast.LENGTH_SHORT).show();
    }
```

## Sending messages

You can easily send a text message using the `sendMessage()` method. It's 4
parameters are:
- A string with the text message
- A string with the session ID of the user who will receive the message
- A string with the message to show in the push notification
- A `JsonObject` with additional parameters to send. The receiver will get the
  exact same `JsonObject in the `params` attribute of the `MOKMessage` class.
  This allows you to send customized messages.

The method immediately returns the message as a `MOKMessage` object as it
asynchronously sends it into the network and stores it into the database using
the store message method that you implemented. this `MOKMessage` object is 
important because it contains the message ID and the timestamp that MonkeyKit 
has given to your message. You can send a "Hello World" message like this:

```
MyMessage newMessage = new MyMessage("Hello World!");
JsonObject params = createJsonWithAdditionalParams();
MOKMessage sentMessage = MonkeyKit.instance().sendMessage(newMessage.getText(),
friend.getSessionID(), "You have received a message via MonkeyKit!", params);
newMessage.setID(sentMessage.getMessage_id());
```
### Sending files

If you want to send a photo or a voice note you should use the
`sendFileMessage()` method. Its 5 parameters are: 
- A string with the absolute path to the file to upload
- A string with the session ID of the user who will receive the message
- A string with the file type. You should use `MessageTypes.FileTypes.Audio` for
  audio messages and `MessageTypes.FileTypes.Photo` for Images
- A `JsonObject` with additional parameters to send. The receiver will get the
  exact same `JsonObject in the `params` attribute of the `MOKMessage` class.
  This allows you to send customized messages.
- A string with the message to show in the push notification

The method immediately returns the message as a `MOKMessage` object as it
asynchronously sends it into the network and stores it into the database using
the store message method that you implemented. this `MOKMessage` object is 
important because it contains the message ID and the timestamp that MonkeyKit 
has given to your message. Here's an example for a voice note message:

```
//send voice note's duration in params
JsonObject params = new JsonObject();
params.addProperty("duration", voiceNote.getDuration()); 
MOKMessage sentMessage =
MonkeyKit.instance().sendFileMessage(voiceNote.getAbsolutePath(), 
friend.getSessionID(), "You have received a voice note via MonkeyKit!", params);
```

## Sending notifications

Sometimes you want to send data in real time to other users, but this data is only
useful if both parties are online. For example you might to notify other users
that you just got online, or you just changed your profile picture. It's better
to not receive these things than to receive them late, because by then the data
might be out of date. MonkeyKit does not persist notifications. MonkeyKit's 
notifications are exactly this. To send a notification, use the 
`sendNotification()` method that has the following 3 parameters:
- A string with the session ID of the user who will receive the notifications
- A `JsonObject` with the data  to send. The receiver will get the
  exact same `JsonObject in the `params` attribute of the `MOKMessage` class.
- A string with the message to show in the push notification

Here's an example of a notification that informs other users that you are
currently online:

```
JsonObject params = new JsonObject();
//online = 1; offline = 0
params.addProperty("online", 0); 
MonkeyKit.instance().sendNotification(friend.getSessionID(), params, "Your best
friend is online!");
```
Notifications are received as `MOKMessage` objects in the
`onNotificationReceived()` callback of `MonkeyKitDelegate`. You can handle
the previously sent notification like this:

```
@Override
    public void onNotificationReceived(MOKMessage message) {
        String friend = getFriendBySessionID(message.getSid());
        String text = null;
        if(message.getParams().get("online").getAsInt() == 1)
            text = " is online";
        else
            text = " is offline";
        Toast.makeText(this, friend + text, Toast.LENGTH_SHORT).show();
```
