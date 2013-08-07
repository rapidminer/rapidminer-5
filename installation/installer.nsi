; -------------------------------------------------
; Basic Definitions
; -------------------------------------------------
 
# Name of Program
Name "RapidMiner 5"

# force compression with lzma
SetCompress force
SetCompressor /SOLID lzma

# No branding text
BrandingText " "

# Request execution level
RequestExecutionLevel admin

# Defines
!include version.nsi
!define REGKEY "SOFTWARE\Rapid-i\$(^Name)"
!define COMPANY "Rapid-I GmbH"
!define URL http://www.rapidminer.com
!define LICENSE ..\release\files\LICENSE.txt
!ifdef WIN64
    !define DEFAULT_INSTALL_DIR $PROGRAMFILES64\Rapid-I\RapidMiner5
    !define OUTPUT_FILE ..\release\rapidminer-${SHORT_VERSION}x64-install.exe
!else
    !define DEFAULT_INSTALL_DIR $PROGRAMFILES\Rapid-I\RapidMiner5
    !define OUTPUT_FILE ..\release\rapidminer-${SHORT_VERSION}x32-install.exe
!endif
!define ALL_FILES_LOCATION ..\release\files\*
!define PROGRAM_EXE RapidMiner.exe
!define BATCH_FILE RapidMinerGUI.bat


; -------------------------------------------------
; Installer Definitions
; -------------------------------------------------

# MUI defines
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\orange-install.ico"
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\orange-uninstall.ico"
!define MUI_UNFINISHPAGE_NOAUTOCLOSE

!define MUI_DIRECTORYPAGE_TEXT_TOP "Please select the folder where $(^Name) should be installed. If you are a user without administrator rights you should select a directory into which you can write, e.g. a directory in your home directory. Installing into a writable directory is especially important if you want to use the automatic update service of $(^Name) without having administrator rights."

# Included files
!include Sections.nsh
!include MUI.nsh
!include "x64.nsh"


# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE ${LICENSE}
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
!insertmacro MUI_LANGUAGE English

# Installer attributes
OutFile "${OUTPUT_FILE}"
InstallDir "${DEFAULT_INSTALL_DIR}"
CRCCheck on
XPStyle on
ShowInstDetails hide
VIProductVersion ${LONG_VERSION}
VIAddVersionKey ProductName $(^Name)
VIAddVersionKey ProductVersion "${LONG_VERSION}"
VIAddVersionKey CompanyWebsite "${URL}"
VIAddVersionKey FileVersion ""
VIAddVersionKey FileDescription ""
VIAddVersionKey LegalCopyright ""
InstallDirRegKey HKCU "${REGKEY}" Path
ShowUninstDetails show


;____________________________________________________________________________
;                            INSTALLER SECTIONS
;____________________________________________________________________________
;

# Installation Files
Section -Main SEC0000
  
    SetOutPath $INSTDIR
    SetOverwrite on
    
    # copy files
    File /r "${ALL_FILES_LOCATION}"
    
    # create shortcuts
    SetShellVarContext all
    
    CreateDirectory "$SMPROGRAMS\$(^Name)"
    CreateShortcut "$SMPROGRAMS\$(^Name)\$(^Name).lnk" $INSTDIR\${PROGRAM_EXE}"
    CreateShortcut "$SMPROGRAMS\$(^Name)\$(^Name) (console).lnk" "$INSTDIR\scripts\${BATCH_FILE}"
    CreateShortcut "$SMPROGRAMS\$(^Name)\Readme.lnk" "$INSTDIR\README.txt"
    CreateShortcut "$SMPROGRAMS\$(^Name)\$(^Name) web site.lnk" "${URL}"
    CreateShortcut "$DESKTOP\$(^Name).lnk" "$INSTDIR\${PROGRAM_EXE}"
    
    # Uninstaller regestry info
    WriteRegStr HKCU "${REGKEY}\Components" Main 1
SectionEnd

# Installation Uninstaller
Section -post SEC0001

    WriteRegStr HKCU "${REGKEY}" Path $INSTDIR
    WriteUninstaller $INSTDIR\uninstall.exe
    
    # create shortcut for uninstaller
    SetShellVarContext all
    
    CreateShortcut "$SMPROGRAMS\$(^Name)\Uninstall $(^Name).lnk" $INSTDIR\uninstall.exe

    # uninstaller registry settings
    WriteRegStr HKCU "SOFTWARE\RapidI\$(^Name)\CurrentVersion\$(^Name)" "$(^Name) Home" $INSTDIR
    WriteRegStr HKCU "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayName "$(^Name)"
    WriteRegStr HKCU "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayVersion "${SHORT_VERSION}"
    WriteRegStr HKCU "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" URLInfoAbout "${URL}"
    WriteRegStr HKCU "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayIcon $INSTDIR\uninstall.exe
    WriteRegStr HKCU "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" UninstallString $INSTDIR\uninstall.exe
    WriteRegDWORD HKCU "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoModify 1
    WriteRegDWORD HKCU "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoRepair 1
SectionEnd

# Macro for selecting uninstaller sections
!macro SELECT_UNSECTION SECTION_NAME UNSECTION_ID
    Push $R0
    ReadRegStr $R0 HKCU "${REGKEY}\Components" "${SECTION_NAME}"
    StrCmp $R0 1 0 next${UNSECTION_ID}
    !insertmacro SelectSection "${UNSECTION_ID}"
    GoTo done${UNSECTION_ID}
next${UNSECTION_ID}:
    !insertmacro UnselectSection "${UNSECTION_ID}"
done${UNSECTION_ID}:
    Pop $R0
!macroend

# Uninstaller sections
Section /o un.Main UNSEC0000
    # uninstall links from start menu and desktop, directory is removed after uninstaller
    SetShellVarContext all
    
    Delete "$SMPROGRAMS\$(^Name)\$(^Name) web site.lnk"
    Delete "$SMPROGRAMS\$(^Name)\Readme.lnk"
    Delete "$SMPROGRAMS\$(^Name)\$(^Name).lnk"
    Delete "$SMPROGRAMS\$(^Name)\$(^Name) (console).lnk"
    Delete "$DESKTOP\$(^Name).lnk"
    
    # delete files
    # RmDir /r /REBOOTOK $INSTDIR  <-- easy but delete also non-RM files
    delete $INSTDIR\.classpath
    delete $INSTDIR\.project
    delete $INSTDIR\build.xml
    delete $INSTDIR\CHANGES.txt
    delete $INSTDIR\INSTALL.txt
    delete $INSTDIR\LICENSE.txt
    delete $INSTDIR\RapidMiner.exe
    delete $INSTDIR\README.txt
    
    RmDir /r $INSTDIR\etc
    RmDir /r $INSTDIR\jre
    RmDir /r $INSTDIR\lib
    RmDir /r $INSTDIR\licenses
    RmDir /r $INSTDIR\resources
    RmDir /r $INSTDIR\sample
    RmDir /r $INSTDIR\scripts
    RmDir /r $INSTDIR\src
    
    # delete uninstaller info registry entry
    DeleteRegValue HKCU "${REGKEY}\Components" Main
SectionEnd

Section un.post UNSEC0001
    DeleteRegValue HKCU "SOFTWARE\RapidI\$(^Name)\CurrentVersion\$(^Name)" "$(^Name) Home"
    DeleteRegKey HKCU "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
    Delete "$SMPROGRAMS\$(^Name)\Uninstall $(^Name).lnk"
    Delete /REBOOTOK $INSTDIR\uninstall.exe
    DeleteRegValue HKCU "${REGKEY}" Path
    DeleteRegKey /IfEmpty HKCU "${REGKEY}\Components"
    DeleteRegKey /IfEmpty HKCU "${REGKEY}"
    
    # remove start menu and installation directories
    SetShellVarContext all
        
    RmDir "$SMPROGRAMS\$(^Name)"
    RmDir /REBOOTOK $INSTDIR
SectionEnd


# ==========================
# Other Installer functions
# ==========================

Function .onInit
   
  InitPluginsDir
 
  ReadRegStr $R0 HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" "UninstallString"
  StrCmp $R0 "" done
 
  MessageBox MB_OKCANCEL|MB_ICONEXCLAMATION \
  "$(^Name) is already installed. $\n$\nClick `OK` to remove the \
  previous version or `Cancel` to cancel this upgrade." \
  IDOK uninst
  Abort
  
  ;Run the uninstaller
  uninst:
  ClearErrors
  ExecWait '$R0 _?=$INSTDIR' ;Do not copy the uninstaller to a temp file
 
  IfErrors no_remove_uninstaller
    ;You can either use Delete /REBOOTOK in the uninstaller or add some code
    ;here to remove the uninstaller. Use a registry key to check
    ;whether the user has chosen to uninstall. If you are using an uninstaller
    ;components page, make sure all sections are uninstalled.
  no_remove_uninstaller:
  
  done:
  
FunctionEnd

# Uninstaller functions
# ========================

Function un.onInit
    ReadRegStr $INSTDIR HKCU "${REGKEY}" Path
    !insertmacro SELECT_UNSECTION Main ${UNSEC0000}
FunctionEnd
