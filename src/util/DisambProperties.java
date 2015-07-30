package util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class DisambProperties {

	private Properties props = null;
	private static DisambProperties disamProps = null;

	public static DisambProperties getInstance() {
		return disamProps;
	}

	public void setProps(Properties props) {
		this.props = props;
	}

	public String getTaggerModel() {
		return props.getProperty("model.tagger");
	}

	public static void init(String propsFile) throws FileNotFoundException,
			IOException {
		disamProps = new DisambProperties();
		Properties props = new Properties();
		props.load(new FileInputStream(propsFile));
		disamProps.setProps(props);
	}

	public String getwikiMinerConfigFile() {
		return props.getProperty("wikiminer.config");
	}

	public String getWordnetDatabaseDir() {
		return props.getProperty("wordnet.dir");
	}

	public String getMSNBCxmlFilesFolder() {
		return props.getProperty("MSNBC.xmlFilesFolder");
	}

	public String getMSNBCtextFilesFolder() {
		return props.getProperty("MSNBC.textFilesFolder");
	}

	public String getAQUAINTxmlFilesFolder() {
		return props.getProperty("AQUAINT.xmlFilesFolder");
	}

	public String getAQUAINTtextFilesFolder() {
		return props.getProperty("AQUAINT.textFilesFolder");
	}

	public String getMSNBCdataset() {
		return props.getProperty("MSNBC.dataset");
	}

	public String getWIKIdataset() {
		return props.getProperty("WIKIcur.dataset");
	}

	public String getIITBdataset() {
		return props.getProperty("IITB.dataset");
	}

	public String getIITBcurdataset() {
		return props.getProperty("IITBcur.dataset");
	}

	public String getAQUAINTdataset() {
		return props.getProperty("AQUAINT.dataset");
	}

	public boolean isNodeOnly() {
		return Boolean.valueOf(props.getProperty("train.nodeonly"));
	}

	public String getkddSpotFilesPath() {
		return props.getProperty("kdd.spotFiles");
	}

	public String getwikiSpotFilesPath() {
		return props.getProperty("wiki.spotFiles");
	}

	public String getkddOriginalGroundObjectFile() {
		return props.getProperty("kddOriginal.groundObject");
	}

	public String getkddCuratedObjectFolder() {
		return props.getProperty("kddCurated.groundObject");
	}

	public String getwikiCuratedObjectFolder() {
		return props.getProperty("wikiCurated.groundObject");
	}

	public String getmsnbcObjectFolder() {
		return props.getProperty("msnbc.groundObject");
	}

	public String getaquaintObjectFolder() {
		return props.getProperty("aquaint.groundObject");
	}

	public String getTrainLRDataset() {
		return props.getProperty("dataset.train.lr");
	}

	public String getLRModel() {
		return props.getProperty("model.lr");
	}

	public String getCompleteIndex() {
		return props.getProperty("index.complete");
	}

	public String getRedirectIndex() {
		return props.getProperty("index.redirect");
	}

	public String getInlinkIndex() {
		return props.getProperty("index.inlink");
	}

	public String getDisambIndex() {
		return props.getProperty("index.disamb");
	}

	public String getAnchorIndex() {
		return props.getProperty("index.anchor");
	}

}
