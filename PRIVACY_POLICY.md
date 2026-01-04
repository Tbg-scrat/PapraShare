# Privacy Policy for Papra Share

**Last Updated: January 2026**

This Privacy Policy describes how the **Papra Share** Android application ("the App") handles your data. The App is designed as a utility to upload user-selected content to a user-configured "Papra" server instance.

### 1. Data Collection and Processing
The App is built on the principle of data minimality. 
* **User-Initiated Sharing:** The App only processes data (files, images, or text) that you explicitly select and share through the Android system share menu. 
* **No Background Collection:** The App does not collect any data in the background, nor does it track your location, contacts, or device usage.

### 2. Data Transmission
* **Destination:** All shared content is transmitted directly from your device to the **Server URL** you have provided in the App settings. 
* **Control:** You have full control over where your data is sent. The App does not send your data to the developers or any third-party servers not specified by you.
* **Encryption:** Content is transmitted via HTTPS (recommended) or HTTP, depending on your manual configuration in the App settings.

### 3. Local Storage and Security
* **Configuration Data:** To function, the App stores your Server URL, API Key, and Organization ID. 
* **Encryption at Rest:** This sensitive configuration data is stored locally on your device using **EncryptedSharedPreferences** (AES-256 encryption).
* **No Cloud Backups:** The App is configured to exclude your sensitive settings from Android's system backups to prevent API keys from being uploaded to cloud services.

### 4. Third-Party Services
The App **does not** contain:
* Third-party analytics (e.g., Google Analytics, Firebase).
* Advertising frameworks.
* Tracking cookies or scripts.

### 5. Your Rights and Data Deletion
Since the App does not store your shared files on its own servers (it only acts as a conduit to your private instance), your data is subject to the privacy settings of your specific Papra server. You can delete the locally stored configuration at any time by clearing the App's cache/data or uninstalling the App.

### 6. Contact
If you have questions regarding this Privacy Policy, you can reach out via the official project repository on GitHub.
