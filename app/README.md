This app works with an accompanying server.

No data is held locally. Upon startup, the MainActivity pings a server and then launches an activity
depending on the server response. The response is JSON containing a "state". When an activity
finishes, the app will contact the server again to get its new state.

LOGIN
"state":"no_user" launches a login screen.
This response should also contain "machine" and "ip".
The login screen allows a user to login according to his user id and PIN (both numeric). The server
handles authentication.

NO JOB
"state":"no_job" launches the activity to start a new job.
This requests information from the user to start a new job, which is then sent to the server.

SETTING IN PROGRESS
"state":"setting" launches a "setting" activity, a special type of job.
This response should also contain "job_number" and "colour".
The end job button requests additional information from the user before communicating to the server.

JOB IN PROGRESS
"state": "active_job" launches the job in progress activity.
This response should also contain "current_activity", "activity_codes" (list) and "colour"
This activity allows the user to switch the current state of the machine according to the list of
activity codes sent by the server. The end job button requests additional information from the user
before communicating to the server.