package wikiGroundTruth.berkeleyDb;

import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;

@Entity
public class AnchorText {

	public String anchor = new String();
	public String context = new String();
	@PrimaryKey
	public long srno;

	@SecondaryKey(relate = MANY_TO_ONE)
	public String entity = new String();

}
