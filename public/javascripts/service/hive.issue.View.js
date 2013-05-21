/**
 * @(#)hive.issue.View.js 2013.03.13
 *
 * Copyright NHN Corporation.
 * Released under the MIT license
 * 
 * http://hive.dev.naver.com/license
 */

(function(ns){
	
	var oNS = $hive.createNamespace(ns);
	oNS.container[oNS.name] = function(htOptions){
		
		var htVar = {};
		var htElement = {};

		/**
		 * initialize
		 * @param {Hash Table} htOptions
		 */
		function _init(htOptions){
			_initVar(htOptions || {});
			_initElement(htOptions || {});

			_initFileUploader();
			_initFileDownloader();
			_setLabelTextColor();
			
			_attachEvent();
		}

		/**
		 * 변수 초기화
		 * initialize variables except HTML Element
		 */
		function _initVar(htOptions){
			htVar.sTplFileItem = $('#tplAttachedFile').text();
			htVar.sAction = htOptions.sAction;

			htVar.htOptChosen = {"allow_single_deselect":true};
		}
		
		/**
		 * 엘리먼트 변수 초기화
		 * initialize HTML Element variables
		 */
		function _initElement(htOptions){
			htElement.welTarget = $("#upload");
			htElement.welTextarea = $("#comment-editor");
			
			htElement.welAttachments = $(".attachments");
			htElement.welLabels = $('.issue-label');
			
			htElement.welSelectAssignee = $("#assigneeId");
			htElement.welSelectMilestone = $("#milestoneId"); 
		}

		/**
		 * 파일 업로더 초기화
		 * initialize fileUploader
		 */
		function _initFileUploader(){
			hive.FileUploader.init({
				"elTarget": htElement.welTarget,
				"elTextarea": htElement.welTextarea,
				"sTplFileItem": htVar.sTplFileItem,
				"sAction": htVar.sAction
			});
		}
		
		/**
		 * 파일 다운로더 초기화
		 * initialize fileDownloader
		 */
		function _initFileDownloader(){
			htElement.welAttachments.each(function(n, el){
				fileDownloader($(el), htVar.sAction);
			});
		}
        
		/**
		 * 라벨 글자색을 배경색에 맞추어 변화시키는 함수
		 * set Labels foreground color as contrast to background color 
		 */
		function _setLabelTextColor(){
			var welLabel;
			var sBgColor, sColor;
			
			htElement.welLabels.each(function(nIndex, elLabel){
				welLabel = $(elLabel);
				sBgColor = welLabel.css("background-color");
				sColor = $hive.getContrastColor(sBgColor); 
				welLabel.css("color", sColor);
			});
			
			welLabel = null;
		}
		
		/**
		 * attachEvent
		 * 이벤트 핸들러 설정
		 */
		function _attachEvent(){
		    htElement.welSelectAssignee.chosen(htVar.htOptChosen).change(_onChangeAssignee);
		    htElement.welSelectMilestone.chosen(htVar.htOptChosen).change(_onChangeMilestone);
		}
		
		/**
		 * onChangeAssignee
		 * 담당자 변경시 이벤트 핸들러
		 */
		function _onChangeAssignee(){
		    console.log($(this).val());
		}
		
		/**
		 * onChangeMilestone
		 * 마일스톤 변경시 이벤트 핸들러
		 */
		function _onChangeMilestone(){
		    console.log($(this).val());
		}
		
		
        _init(htOptions || {});
	};
	
})("hive.issue.View");