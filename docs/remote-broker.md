# Optional central trainer broker

Local loopback is the three-station pilot default. This optional profile runs the same Java broker on Linux with the same client JARs. It does not deploy/configure a real server. A trainer-operated training process needs its own runtime directory and JDK 21; never reconfigure a production broker for this lab.

## Trainer preparation on Linux

Extract the runtime ZIP, run without root, and keep data inside its owned runtime/. Copy config/remote.properties to config/remote.local.properties and edit local values:

```properties
profile=remote
host=broker.training.example
bind=0.0.0.0
port=61617
namespace=trainer
ssl=true
keyStore=config/server.p12
usersFile=config/users.properties
```

An explicit bind is required. Use 0.0.0.0 only on a server approved for training, or select its specific IP. Agree firewall, DNS, certificate name and TLS TCP port with IT. There is no web console, HTTP management port or trainee management permission.

Provide a PKCS12 keystore containing the private key and full certificate chain. The certificate needs the actual DNS name as a Subject Alternative Name. Check expiry before training. Keep hostname verification enabled; no trustAll or plaintext fallback. An isolated local keytool test may use a dedicated training CA/test certificate whose agreed root is explicitly trusted; do not import arbitrary unverified server certificates.

Create individual users in untracked config/users.properties (placeholders only):

```properties
alice.password=<SET_INDIVIDUALLY_AND_LOCALLY>
alice.namespace=alice
bob.password=<SET_A_DIFFERENT_PASSWORD_LOCALLY>
bob.namespace=bob
# Optional, only for intentionally shared exercises:
# alice.groups=team
# bob.groups=team
```

Restrict local configuration, users and key files to the trainer (`chmod 600`). This small startup-loaded training file is not a production identity provider; use synthetic training accounts. Roles allow sending, receiving, browsing and destination lifecycle only within authorized namespaces/groups. Management stays in the trainer's local broker process. Remote error addresses are also namespace-bound: `<namespace>.sNN.DLQ` and `.EXPIRY`, with per-source queue names. Other users' error areas remain inaccessible.

Set keyStorePassword in protected local configuration or LAB_KEY_STORE_PASSWORD, never CLI arguments/history. Start in the foreground:

```sh
./scripts/broker.sh start --config config/remote.local.properties
```

Wait for BROKER_READY. From another server terminal, the trainer can run `./scripts/broker.sh stop --config config/remote.local.properties`; wait for actual exit before restarting with the same config/data path. The broker prints read-only depth, consumers and delivering status. Each instance needs a separate runtime root; the file lock intentionally permits one broker per lab directory.

## Company-PC client preparation

The trainer supplies the verified truststore/CA, hostname, port, namespace and individual user. Copy the remote configuration locally:

```properties
profile=remote
host=broker.training.example
port=61617
namespace=alice
ssl=true
user=alice
trustStore=config/training-trust.p12
```

Set password and trustStorePassword only in a protected *.local.properties file or LAB_PASSWORD/LAB_TRUST_STORE_PASSWORD. There is no local demo fallback. Remote requests with the local profile file are refused. Precedence remains CLI > LAB_ environment > selected file > secure defaults; share secrets through protected files/environment, not CLI. Relative paths resolve from the lab root.

Before training, in cmd.exe:

```bat
scripts\doctor.cmd runtime --config config\remote.local.properties
```

The bounded probe checks TCP, TLS certificate/hostname and authentication. Also send/receive one message in your own area to check authorization:

```bat
scripts\lab.cmd s01 produce --count 1 --config config\remote.local.properties
scripts\lab.cmd s01 consume --count 1 --config config\remote.local.properties
```

The automated suite uses temporary local certificates and checks wrong hostnames, unauthorized namespaces, groups and foreign DLQs. It cannot replace a real company-PC-to-trainer-server preflight.

## Shared work and limits

A/S02 needs the same authorized group queue: trainer grants groups=team, then both clients use --namespace team. B/S03 needs one authorized topic and independent subscriptions. Personal runs remain in alice/bob. E/H2 stores a local ledger per machine, not distributed idempotency.

An HTTPS website loading does not establish broker connectivity. CORE TCP is not HTTP; a port change does not bypass a required HTTP proxy. If company firewall/proxy policy blocks access, use the local broker or prebuilt ZIP. Do not bypass company controls.

Never reset a shared remote broker for individual cleanup. Consume known owned work, unsubscribe your own durable identity, and remove an empty owned queue with cleanup --confirm. Only the trainer may reset its owned server area after **all** users have finished. Remote deployment/configuration is separately authorized work.
