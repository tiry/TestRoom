<html>
<#assign folders = session.query("select * from Folder where ecm:parentId='" + room.id  + "'")/>

 <#list folders as folder>

    <#assign children = session.query("select * from Document where ecm:ancestorId ='" + folder.id + "'")/>
    <ul>
    <#list children as child>
    
       <li>${child.title} - ${child.dublincore.format} - ${child.dublincore.language} - ${child.dublincore.contributors} - ${child.common.size} - ${child.uid.uid}</li>
 
    </#list>
    </ul>
 </#list>

<br/>
Rendering Time : ${This.getTimeDiff(t0)}

</html> 