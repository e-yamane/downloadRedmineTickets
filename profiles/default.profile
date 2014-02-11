redmine {
	url = 'スラッシュで終わるとエラーです。'
	accessKey = 'アクセスキーを指定'
	projectKey = 'プロジェクト名'
	queryId = ''
	includes = 'journals'	//attachments changesets journals relations watchers 
}

sorterRef='${sorter.チケット登録順}'

localFilter {
	未完了または特定スプリントの特定トラッカーのチケット = '''
		tracker.name == '${trackerName}' && (statusName != 'Done' || targetVersion == null || targetVersion.name == '${versionName}')
'''
}

sorter {
	チケット登録順 = '#{"id":"asc"}'
}

flatter = '''
#{
	"No.":id, 
	"チケット名":subject, 
	"状態":statusName, 
	"チケット作成日":#util.date(createdOn, "yyyy/M/d"), 
	"チケット作成者":author.fullName,
	"最終更新日":#util.date(updatedOn, "yyyy/M/d"),
	"スプリント":(targetVersion == null) ? "" : targetVersion.name,
	"メモ":#util.join(
	    journals.{? 
	        (#this.notes != null && #this.notes.isEmpty() == false)
	    }.{
	        #util.date(#this.createdOn, 'yyyy/MM/dd') + ':' + #this.user.fullName + '\\n' + #this.notes
	    }, \"\\n\")
}
'''