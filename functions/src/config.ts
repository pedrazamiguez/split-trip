import { setGlobalOptions } from "firebase-functions/v2";

// Configure default region to match the eur3 database triggers
setGlobalOptions({ region: "europe-west1" });
