package org.powertac.experiment.models;

import org.powertac.experiment.beans.Experiment;
import org.powertac.experiment.beans.Game;
import org.powertac.experiment.beans.Study;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "parameters")
public class Parameter
{
  private int parameterId;
  private Study study;
  private Experiment experiment;
  private Game game;
  private String type;
  private String value;

  public Parameter ()
  {

  }

  public Parameter (Object owner, String type, Object value)
  {
    setOwner(owner);
    this.type = type;
    this.value = String.valueOf(value);
  }

  private void setOwner (Object owner)
  {
    if (owner instanceof Study) {
      this.study = (Study) owner;
    }
    else if (owner instanceof Experiment) {
      this.experiment = (Experiment) owner;
    }
    else if (owner instanceof Game) {
      this.game = (Game) owner;
    }
  }

  //<editor-fold desc="Getters and Setters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "parameterId", unique = true, nullable = false)
  public int getParameterId ()
  {
    return parameterId;
  }

  public void setParameterId (int parameterId)
  {
    this.parameterId = parameterId;
  }

  @ManyToOne
  @JoinColumn(name = "studyId")
  public Study getStudy ()
  {
    return study;
  }

  public void setStudy (Study study)
  {
    this.study = study;
  }

  @ManyToOne
  @JoinColumn(name = "experimentId")
  public Experiment getExperiment ()
  {
    return experiment;
  }

  public void setExperiment (Experiment experiment)
  {
    this.experiment = experiment;
  }

  @ManyToOne
  @JoinColumn(name = "gameId")
  public Game getGame ()
  {
    return game;
  }

  public void setGame (Game game)
  {
    this.game = game;
  }

  @Column(name = "type", nullable = false)
  //@Enumerated(EnumType.STRING)
  public String getType ()
  {
    return type;
  }

  public void setType (String type)
  {
    this.type = type;
  }

  @Column(name = "value", nullable = false)
  public String getValue ()
  {
    return value;
  }

  public void setValue (String value)
  {
    this.value = value;
  }
  //</editor-fold>

  //<editor-fold desc="Static stuff">
  public static List<ParamEntry> getDefaultList ()
  {
    List<ParamEntry> defaultList = new ArrayList<>();

    Type[] types = {Type.brokers(), Type.reuseBoot(), Type.location(),
        Type.simStartDate(), Type.multiplier()};

    for (Type type : types) {
      defaultList.add(new ParamEntry(type.name, type.getDefault()));
    }

    return defaultList;
  }

  public static List<ParamEntry> getParamList (ParamMap map, int minLength)
  {
    List<ParamEntry> paramList = new ArrayList<>();

    for (Map.Entry<String, Parameter> entry : map.entrySet()) {
      if (Arrays.asList(Type.pomId, Type.seedList ).contains(entry.getKey())) {
        continue;
      }
      paramList.add(new ParamEntry(entry.getKey(), entry.getValue().value));
    }
    while (paramList.size() < minLength) {
      paramList.add(new ParamEntry("", ""));
    }

    return paramList;
  }

  public static List<String[]> getAvailableServerParams (int pomId)
  {
    List<String[]> availableParams = new ArrayList<>();

    for (Type type : Type.getTypes(pomId)) {
      if (type.name.equals(Type.createTime) ||
          type.name.equals(Type.startTime)) {
        continue;
      }

      availableParams.add(type.getStringArray());
    }

    availableParams.sort(Comparator.comparing(p -> p[0]));

    return availableParams;
  }

  public static List<String[]> getAvailableBaseParams ()
  {
    List<String[]> availableParams = new ArrayList<>();

    for (Type type : Type.getBaseTypes()) {
      if (type.name.equals(Type.createTime) ||
          type.name.equals(Type.startTime)) {
        continue;
      }

      availableParams.add(type.getStringArray());
    }

    availableParams.sort(Comparator.comparing(p -> p[0]));

    return availableParams;
  }
  //</editor-fold>
}
