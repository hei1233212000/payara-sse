#### This project shows the issue of javax.ws.rs.sse.SseBroadcaster.onClose() where it does NOT triggered when client side close the connection

### Steps to reproduce
1. run the application `gradlew clean build runApp`
2. go to http://localhost:8080/payara-sse/
3. click the "Subscribe" button, then you can see the server log will show subscription is done
4. open the other browser and repeat the steps 2 and 3
5. click the "Increase Counter" button, you will see the counter in the BOTH web page will be increased and there is a log message in server console too
6. now click the "Unsubscribe" button in either one web page, **there is NO log printed in the server side which is NOT expected**
7. Go to the web page which is still subscribing the stream and click the "Increase Counter" button
8. So, the counter in the page is still subscribing the stream will be updated; the counter in the page is NOT subscribing the stream will remain. It show that the "unsubscribe" is working and the stream connection should be closed by client side
9. For double check, you could ge to http://localhost:8080/payara-sse/api/counter/event-status to check if there is any event stream is closed (true means closed)