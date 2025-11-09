A plugin that can be used to audit every region in a Minecraft server
This is primarily developed for use in BTE (Build The Earth), but may be used in other servers, provided credit has been provided.

Notes:
-When first using the command '/reloaddatabase' must be executed, or errors will occur when trying to use the other commands
-Make sure to read the descriptions in the 'plugin.yaml' file
-This is a completely manual plugin, and for large servers, it may take a while for all regions to be audited by server staff members
-When marking a region as 'MFD' (Mark for Deletion), it will TP the player to a set position in the overworld (this is buildhub on BTE UK)
  -Feel free to change these coordinates if required ('markRegion.java' line 87)

Warnings:
-It is not recommended to run '/reloaddatabase' on large servers when there are large volumes of players online, this is because I have not done the searching/SQL updating asynchronously
-Again, read 'plugin.yaml' command descriptions

This is my first proper large-scale plugin, so errors/bugs are expected; use this plugin at your own risk.
