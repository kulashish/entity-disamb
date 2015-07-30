<%@ page import = " javax.servlet.*, javax.servlet.http.*, java.io.*, java.net.URLEncoder, java.util.ArrayList, java.util.HashMap, util.* ,spotting.*, disamb.*;" %>

<%!
public String escapeHTML(String s) {
  s = s.replaceAll("&", "&amp;");
  s = s.replaceAll("<", "&lt;");
  s = s.replaceAll(">", "&gt;");
  s = s.replaceAll("\"", "&quot;");
  s = s.replaceAll("'", "&apos;");
  return s;
}
%>
<%@include file="header.jsp"%>
 
<%	
	String queryString = request.getParameter("query");
	String maxnodes = request.getParameter("maxnodes");
	 int consolidationSize = 6;
	 int csize=10;
%>
<p>Annotate text with Wikipedia entities: </p>
	<form name="search" action="index.jsp" method="post">
		<p>
			Document Text:<br>
			<textarea name="query" cols="60" rows="6">
<%
			if (queryString != null && !"".equals(queryString)) out.print(queryString);
			else out.print("India comprises the bulk of the Indian subcontinent and lies atop the minor Indian tectonic plate, which in turn belongs to the Indo-Australian Plate.");
%>
			</textarea>
		</p>
		<p>
			maxNodes: &nbsp; <input name="maxnodes" size="5" value="5"/><br>
			<small>Max number of entities to be retrieved <b>per mention</b>. Choose value from 1-100.</small>
		</p>
		<p>
			<input type="submit" value="Annotate"/>
		</p>
    </form>
<hr />
<%
	if (queryString != null && !"".equals(queryString)) {
		ExtractKeywordsGroundTruth kw_extractor = new ExtractKeywordsGroundTruth();
		KeywordsGroundTruth kw = kw_extractor.extractFromString(queryString, new ArrayList<String>(),csize);
		FeatureExtractor ft_extractor = new FeatureExtractor();
		int maxNodes;
		try {
			maxNodes = Integer.parseInt(maxnodes);
		} catch (Exception e) {
			maxNodes = 20;
		}
		
		TrainingData tdnew = ft_extractor.extractFeatures(kw, maxNodes,consolidationSize);
		/*FileWriter f2 = new FileWriter("/home/kanika/workspace/EntityDisamb/check/"+"Candidates.txt");
		for(NodePotentials n : tdnew.nodes.potentials_set){
			f2.write(n.mention.split("_")[0]+"	"+Integer.parseInt(n.mention.split("_")[1])+"	"+ n.name+"\n");
		}
		f2.close();*/
		HashMap<String, ArrayList<NodePotentials> > m2e = new HashMap<String, ArrayList<NodePotentials>>();
		for (NodePotentials np : tdnew.nodes.potentials_set) {
			if (m2e.containsKey(np.mention)) {
				m2e.get(np.mention).add(np);
			} else {
				m2e.put(np.mention, new ArrayList<NodePotentials>());
				m2e.get(np.mention).add(np);
			}
		}
		
		QuadraticOptimizer qo = new QuadraticOptimizer();
		qo.loadEntityData(tdnew);
		HashMap<String,Double> disamb_val = qo.runDisambQO();
		ArrayList<String> disamb_hi = new ArrayList<String>();
		ArrayList<String> disamb_lo = new ArrayList<String>();
		if (disamb_val != null){
            for (String node_name: disamb_val.keySet()){
                Double val = disamb_val.get(node_name);
                if (val >= 0.5)
                    disamb_hi.add(node_name);
                else
                    disamb_lo.add(node_name);
            }
        }
%>
<p>Annotated text:</p>
<p><i><%=queryString%></i></p>
<p>MaxNodes: <%=maxnodes%></p>

<%
        if (disamb_val != null){
%>
<table width="60%" cellspacing="2" cellpadding="2" border="1">
    <tr>
        <th>High Scoring Entities (>=0.5)</th>
        <th>Low Scoring Entities (<0.5)</th>
    </tr>
    <tr>
        <td style="vertical-align:top">
        <ul>
<%
            for (String node_name: disamb_hi){
                String wikiname = node_name.replaceAll(" ","_");
%>
            <li><b><a href="http://en.wikipedia.org/wiki/<%=wikiname%>"><%=node_name%></a></b>: <%=disamb_val.get(node_name)%></li>
<%
            }
%>
        </ul>
        </td>
        <td style="vertical-align:top">
        <ul>
<%
            for (String node_name: disamb_lo){
                String wikiname = node_name.replaceAll(" ","_");
%>
            <li><b><a href="http://en.wikipedia.org/wiki/<%=wikiname%>"><%=node_name%></a></b>: <%=disamb_val.get(node_name)%></li>
<%
            }
%>
        </ul>
        </td>
    </tr>
</table>
<%
        }
%>
<hr />

<table width="80%" cellspacing="4" cellpadding="4">
<tr>
	<th>Mention</th>
	<th width="25%">Query</th>
	<th width="50%">Entities</th>
</tr>
		
<%	
		for (String mention : m2e.keySet()) {
%>
<tr>
	<td><%=mention%></td>	
	<td>
		<form>
		<textarea name="lucene_<%=mention%>" cols="60" rows="6"><%=tdnew.nodes.mention_queries.get(mention)%></textarea><br>
		<input type="submit" name="submit_<%=mention%>" value="Query Lucene" />
		</form>
	</td>
	<td>
<%
			for (NodePotentials np : m2e.get(mention)) {
				String wikiname = np.name.replaceAll(" ","_");
%>
	    <a href="http://en.wikipedia.org/wiki/<%=wikiname%>"><%=np.name%></a> (<%=np.context_score%>),&nbsp;
<%
			}
%>
	</td>
</tr>
<%
		}
%>
</table>
<%
	}
%>

<%@include file="footer.jsp"%>
