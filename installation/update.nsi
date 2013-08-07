!include "FileFunc.nsh"

Name RapidMinerUpdate
Caption "RapidMiner Update"
Icon "rapidminer_icon.ico"
OutFile "../release/files/scripts/RapidMinerUpdate.exe"

# Request execution level
RequestExecutionLevel admin

SilentInstall silent
AutoCloseWindow true
ShowInstDetails nevershow

Section ""
	 ;
	 ;$R0 is reserved for compares 
	 ;$R1 is the rapidMiner-directory at the user files
	 ;$R2 is the RapidMiner-directory in program files
	 ;
	 ;get user-directory
     ${GetParameters} $R1
     
     ;get RapidMiner-directory in program files
     StrCpy $R2 $EXEDIR -8
     
	 ; Check if RUinstall contains new RapidMinerUpdate.exe
     StrCpy $R0 "$R1\update\RUinstall\RapidMinerUpdate.exe"
     IfFileExists $R0 UpdateItself SecondCheck
     
     UpdateItself:
        ; copy RapidMinerUpdate and rename it after next systemreboot
        Rename "$R1\update\RUinstall\RapidMinerUpdate.exe" "$R2\RapidMinerUpdate.exex"
        Rename /REBOOTOK "$R2\RapidMinerUpdate.exex" "$R2\RapidMinerUpdate.exe"
       
     SecondCheck:
     	;check whether RUinstall contains a new RapidMiner.exe
     	StrCpy $R0 "$R1\update\RUinstall\RapidMiner.exe"
        IfFileExists $R0 UpdateRapidMiner thirdCheck
        
     UpdateRapidMiner:
        ; copy RapidMiner and rename it after next systemreboot
        Rename "$R1\update\RUinstall\RapidMiner.exe" "$R2\RapidMiner.exex"
        Rename /REBOOTOK "$R2\RapidMiner.exex" "$R2\RapidMiner.exe"
        
     thirdCheck:
     	;check if update contains UPDATE-file 
     	StrCpy $R0 "$R1\update\UPDATE"
        IfFileExists $R0 DeleteFiles DoRest
        
     DeleteFiles:
     	;iterate over UPDATE and move the files with the key (DELETE) to the deleteMe-folder
     	CreateDirectory "$R1\deleteMe"
     	Call WorkUPDATE
		
     DoRest:
        ;copy files from user-profile to program files
        ClearErrors
        CopyFiles /SILENT "$R1\update\RUinstall\*" "$R2"
        
        ;clean up the files
        ClearErrors
        RmDir /r $R1\update
        ClearErrors
        RmDir /r $R1\deleteMe
     
     Quit
   
SectionEnd


Function WorkUPDATE

		;Parse Update-file and copy files and directories to deleteMe
		StrCpy $8 "0" ;counter to exclude name conflicts in deleteMe
		ClearErrors
		FileOpen $4 "$R1\update\UPDATE" "r"
     	FileSeek $4 0

     NextLine:
       ;read line and check whether the keyword is correct
       IntOp $8 $8 + 1
        ClearErrors
   		FileRead $4 $1
   	  	IfErrors DoneLoop
		StrCpy $5 $1 7
		StrCmp $5 "DELETE " DoWork NextLine ;check for the right keyword
		
	DoWork:
	  ;keyword is correct. Copy end of line to a new variable and reforamt the string (cut "DELETE rapidminer/" away)
		StrCpy $6 $1 "" 17
		
	  ;trim newlines
		StrCpy $7 $6 "" -6
		StrCmp $7 "$\r$\n" Equal NotEqual
		
	  Equal:
		StrCpy $7 $6 -6
		Goto ChangeSlashes
		
	  NotEqual:
		StrCpy $7 $6	
		
	ChangeSlashes:	
	  ;changes slashes to backslashes
		Call StrSlash
		
	;do it
		ClearErrors
		rename "$EXEDIR$7" "$R1\deleteMe\$8to-delete"
		IfErrors NextLine
		Goto NextLine
		 
	DoneLoop:
		FileClose $4
		ClearErrors
	
FunctionEnd

Function StrSlash

		StrCpy $R8 "" ;aim register
		
	loop:
		StrCpy $R4 "/" ;char to replace
		StrCpy $R3 "\" ;char to place and the positions
		StrCpy $R5 $7 1 ;write the first char of the leftover to R5
		StrCpy $7 $7 "" 1 ;cuts the first char away
		StrCmp $R5 $R4 found normalChar ;branch to found if R5 contains a slash 
		
	found:
		StrCpy $R8 "$R8$R3" ;add backslash instead of slash
		StrCmp $7 "" done loop
		
	normalChar:	
		StrCpy $R8 "$R8$R5" ;add to aim-string
		StrCmp $7 "" done loop
		
	done:
		StrCpy $7 $R8 ;copy string back
		ClearErrors
		
FunctionEnd
