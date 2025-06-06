package com.advancedcomponentservices.acswarehouse.google

class Endpoints {
    companion object {
        const val PROFILE = "https://gmail.googleapis.com/gmail/v1/users/me/profile"
        const val TOKEN = "https://oauth2.googleapis.com/token"
        const val SEND = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
        const val MESSAGES = "https://gmail.googleapis.com/gmail/v1/users/me/messages/"
        const val HISTORY = "https://www.googleapis.com/gmail/v1/users/me/history"
        const val OAUTH = "https://accounts.google.com/o/oauth2/v2/auth"
        const val THREADS = "https://gmail.googleapis.com/gmail/v1/users/me/threads"
        const val DRIVE = "https://www.googleapis.com/drive/v3/files"
        const val SHEETS = "https://sheets.googleapis.com"
        const val SHEETS_READ = "$SHEETS/v4/spreadsheets/"
        const val BATCH_SHEETS_READ = "$SHEETS/v4/spreadsheets/{spreadsheetId}/values:batchGet"
        const val SHEETS_UPDATE = "$SHEETS/v4/spreadsheets/{spreadsheetId}/values/{range}?valueInputOption={valueInputOption}"
        const val BATCH_SHEETS_UPDATE = "$SHEETS/v4/spreadsheets/{spreadsheetId}:batchUpdate"
        const val APPEND_TO_SHEET = "$SHEETS/v4/spreadsheets/{spreadsheetId}/values/{range}:append"
        const val CLEAR_FROM_SHEET = "$SHEETS/v4/spreadsheets/{spreadsheetId}/values/{range}:clear"
    }
}