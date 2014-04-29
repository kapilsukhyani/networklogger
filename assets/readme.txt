-example_store.bks is the keystore created and managed using Portecle
-cakey.pem is the private key of ca root certificate created using openssl
-careq.pem is the ca root certificate signing request created using openssl and which is used to created
self signed ca root certificate
-caroot.cer is the self signed ca root certificate created using openssl and cakey.pem and careq.pem as parameters
-rand is file used bu openssl to create private key
-serial.txt is also used by openssl to give serial number to certificates from this file and increment it by one, once a number mentioned
in that file assigned to a cert
-www_google_com.cer is a exported certificate from example_store.bks created to fake google's identity
-www_google_com.csr is a certificate signing request for www_google_com.cer which is used to sign www_google_com.cer using cakey.pm and caroot.cer passed as parameters to openssl
-www_google__trusted.cer is the singed version of www_google_com.cer and www_google_com.cer is replaced by www_google__trusted.cer in the example_store.bks,
it can done by right clicking the www_google_com.cer in portecle and selecting "import ca reply" option and then selecting www_google__trusted.cer
-1eb583f9.0 is actually www_google__trusted.cer certificate the only thing is its been renamed with hash value subject name in the certificate. Because
this is how android stores the user added certificate in data/misc/keychain/cacerts-added directory as user trusted
 ca root certificates
References:
http://nelenkov.blogspot.in/2011/12/ics-trust-store-implementation.html
http://nelenkov.blogspot.in/2011/12/ics-credential-storage-implementation.html
https://wiki.cacert.org/FAQ/ImportRootCert?action=show&redirect=ImportRootCert#Android_Phones_.26_Tablets
https://sites.google.com/site/ddmwsst/create-your-own-certificate-and-ca