Duy Tien Nguyen s80287

## Funktionalität der Programme

- Funktion Ihres Clients + Server ohne Fehlersimulation: funktioniert
- Funktion Ihres Clients + Server mit Fehlersimulation: funktioniert
- Funktion Ihres Clients + Server über Hochschulproxy: funktioniert
- Funktion Ihres Clients + Hochschulserver ohne Fehlersimulation: funktioniert
- Funktion Ihres Clients + Hochschulserver mit Fehlersimulation: funktioniert


## How to run this UDP-Protocol

I. Without paket loss and paket delay
1. Open two windows in terminal
2. 1st. window terminal: call server
`bash server.sh 3333`
3. 2nd. window terminal: run client
`bash bash client.sh localhost 3333 test-file.txt`

II. With paket loss and paket delay
1. call server
`bash server.sh 3333 0.1 150`
paket loss = 10%
paket delay = 150ms
2. run client: same as above

III. Hochschulserver without paket loss and paket delay
1. client:
`bash client.sh 141.56.134.111 3330 test-file.txt`
idefix IP-adress: 141.56.134.111

IV. Hochschulserver with paket loss and paket delay
1. client:
`bash client.sh 141.56.134.111 3333 test-file.txt`

V. Hochschulproxy
1. run VPN
2. server
`bash server.sh 3340`
3. client
`bash client.sh localhost 3340 test-file.txt`