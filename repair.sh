#!/bin/bash
function core(){
	for file in `ls $1`
	do
		java -jar apkRepair.jar $src$file
	done
}
src="../apkLibDetect/apks/"
core $src
