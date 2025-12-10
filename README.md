# **Capture the Flag (Revamped)**

**Type:** Web App / Live Game · **Tech Stack:** React, Java, Spring Boot · **Status:** Active

## **Overview**

**Capture the Flag** is a common game played in many fields, including computer science, sports and even military training. This platform has been designed for use as a physical game of Capture the Flag in Sydney, and contains a range of features that make the game run smoothly for players and moderators.

## **Features**

* **Live Updates:** The game states are broadcasted live as they happen through WebSockets. If the game is paused, there is an emergency, the game ends, or any major event happens, all players will immediately be notified (with optional push notifications).
* **Messaging System:** The game has an inbuilt messaging system for a global chat and teams chat (encrypted for team members).
* **Security:** Team chats, flag locations and private information are all encrypted with HTTPS and require a JWT to access.
* **Optimised Website:** The website is designed to be compatible with older and slower devices, including a Virtuoso and smooth infinite scrolling feature for the chats.

## **Developer Features**
* **Debug and Administration:** There are a variety of extra settings for debugging and administrator use including hard-resets, device specs, player removal, game controls and emergency declaration.
* **Flexibility:** The map is defined by a single public file, and can be changed to anything if needed. Same the with the team amounts and team colours.

## **Purpose**
Built for a social event to play amongst friends and even to meet new people. The [original version](https://github.com/Moaesaycto/Capture-The-Flag-Deprecated) was made with the same intention, however this was built with more experience and reliability.

## **Live Demo**

The website is live, however, unless a game is running (or the server is running), it will not be accessible. A live video demonstration will be provided in the near future.

📖 **Read the rules:** [moae.dev/capture-the-flag/#/rules](https://moae.dev/capture-the-flag/#/rules)

🌐 **View the Website:** [moae.dev/capture-the-flag](https://moae.dev/capture-the-flag)

---

## Set-up and Guide
If you are intended on running this system yourself, you are reminded of the following:

> This project is provided "as is" without any warranties or guarantees. Use it at your own risk. I'm not responsible for any damage or issues that may arise from using this project. Contributions and feedback are welcome, though I can't promise I'll have the time to change things around. Most of what I do is for simple and fun projects.

I've made it super simple to set it up on your machine, but if you intend on hosting your own version on the public internet, be very aware of the security risks associated with each key you generate and put in.

Below are the guides for setting it up on your own system. Clone the project and make sure you have [Docker](https://www.docker.com/) installed.

### Setting Up Environment Variables
* Begin by creating a `.env` in the root folder. Inside of which, you will need to fill out the following blocks with your app's 

#### Bridge Information
```env
FRONTEND_URL=<Frontend URL>

VITE_BACKEND_URL=<Backend URL>
VITE_BACKEND_SOCKET_URL=<Backend URL for WebSockets>
```

#### Settings
```env
VITE_JWT_KEY=<Some random key>
VITE_MAX_MESSAGE_LENGTH=256

JWT_SECRET=<Random key of 32 characters>
SSL_ENABLED=true
ERROR_MESSAGES=never
SPRING_SECURITY_DEBUG=false

APP_AUTH_PASSWORD=<Password to become an authorised user in the game>
```

#### SSL and VAPID Keys (See below)
```env
SSL_KEY_ALIAS=api.moae.dev
SSL_KEYSTORE_PASSWORD=<Encryption key>
SSL_KEYSTORE=file:/app/certs/ctf-local.p12
CTF_KEYSTORE_PASSWORD=<Should be the same as SSL_KEYSTORE_PASSWORD>

APPLICATION_SERVER_KEY=<Your VAPID public key>
VAPID_PUBLIC_KEY=<Your VAPID public key (yes, twice)>
VAPID_PRIVATE_KEY=<Your VAPID private key>
VAPID_SUBJECT=mailto:<your email>
```

The above block of code requires a little more work. You can generate your VAPID key very simply using [this online generator](https://vapidkeys.com/). Fill in the above information with that.

As for the keystore file, you will need to generate that yourself. The command you should use will look something like this:

```bash
keytool -genkeypair -alias api.moae.dev -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore CTFBackend\src\main\resources\certs\ctf-local.p12 -storepass <password> -validity 365
```

Follow what it says, and you should end up with a `.p12` file in `CTFBackend\src\main\resources\certs`. Make sure the file is called `ctf-local.p12`, or just change it to match in the `.env` file.

### Running

This application has been Dockerized, which means you simply run:

```bash
docker-compose up --build
```

Access it via the frontend URL you provided in the `.env` file.

### Updating the Game Settings

If you want to change the settings for the game, such as the duration of each period and the teams, you can find the information in `CTFBackend/src/main/resources/config.yml`. You can change the information there if you would like:

```yml
game:
  maxPlayers: -1
  minPlayers: 2
  minPlayersPerTeam: 1
  maxPlayersPerTeam: -1
  maxTeams: 2
  graceTime: 5 # 600 # 10 minutes
  scoutTime: 5 # 1800 # 30 minutes
  ffaTime: 5 # 1200 # 20 minutes
teams:
  - name: Yellow
    color: "#e1ff00"
  - name: Orange
    color: "#ff8300"
  - name: Pink
    color: "#ff00ee"
```