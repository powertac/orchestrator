package org.powertac.experiment.beans;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.experiment.constants.Constants;
import org.powertac.experiment.models.ParamMap;
import org.powertac.experiment.models.ParamMap.MapOwner;
import org.powertac.experiment.models.Parameter;
import org.powertac.experiment.models.Seed;
import org.powertac.experiment.models.Type;
import org.powertac.experiment.services.HibernateUtil;
import org.powertac.experiment.services.MemStore;
import org.powertac.experiment.services.Properties;
import org.powertac.experiment.services.Scheduler;
import org.powertac.experiment.services.Utils;
import org.powertac.experiment.states.ExperimentState;
import org.powertac.experiment.states.GameState;
import org.powertac.experiment.states.StudyState;

import javax.faces.bean.ManagedBean;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;


@ManagedBean
@Entity
@Table(name = "studies")
public class Study implements MapOwner
{
  private static Logger log = Utils.getLogger();

  private int studyId;
  private User user;
  private String name;
  private StudyState state = StudyState.pending;

  private Map<String, Parameter> parameterMap = new HashMap<>();
  private ParamMap paramMap = new ParamMap(this, parameterMap);
  private String variableName;
  private String variableValue;

  public Study ()
  {
  }

  @Transient
  public boolean isEditingAllowed ()
  {
    if (!isOwnerAdmin()) {
      return false;
    }

    return state == StudyState.pending && isOwnerAdmin();
  }

  @Transient
  public boolean isSchedulingAllowed ()
  {
    if (!isOwnerAdmin()) {
      return false;
    }

    return (state == StudyState.paused || state == StudyState.pending);
  }

  @Transient
  public boolean isDeletingAllowed ()
  {
    if (!isOwnerAdmin()) {
      return false;
    }

    // Can't delete if games are still running
    if (hasRunningGames()) {
      return false;
    }

    // Allow deleting when not pending and not running
    return true;
  }

  @Transient
  public boolean isPausingAllowed ()
  {
    if (!isOwnerAdmin()) {
      return false;
    }

    return state == StudyState.in_progress;
  }

  @Transient
  private boolean isOwnerAdmin ()
  {
    User currentUser = User.getCurrentUser();
    return (currentUser.isAdmin() ||
        currentUser.getUserId() == user.getUserId());
  }

  @Transient
  public List<Experiment> getExperiments ()
  {
    List<Experiment> experiments = new ArrayList<>();
    for (Experiment experiment : Experiment.getNotCompleteExperiments()) {
      if (experiment.getStudy().getStudyId() == studyId) {
        experiments.add(experiment);
      }
    }
    return experiments;
  }

  private boolean hasRunningGames ()
  {
    for (Game game : Game.getNotCompleteGamesList()) {
      // Check for running games that belong to this study
      if (GameState.getRunningStates().contains(game.getState()) &&
          game.getExperiment().getStudy().getStudyId() == studyId) {
        return true;
      }
    }
    return false;
  }

  public void pauseStudy ()
  {
    state = StudyState.paused;

    Scheduler scheduler = Scheduler.getScheduler();
    for (Experiment experiment : Experiment.getNotCompleteExperiments()) {
      if (experiment.getStudy().getStudyId() == studyId) {
        scheduler.unloadExperiment(experiment.getExperimentId());
      }
    }
  }

  public void scheduleStudy (Session session)
  {
    if (state == StudyState.pending) {
      String seedList = paramMap.getValue(Type.seedList);
      if (seedList != null && seedList.contains("http")) {
        // We need a separate thread, downloading might take too long
        log.info("Scheduling study in background");
        new Thread(new ScheduleRunnable(studyId, paramMap.clone())).start();
      }
      else {
        actuallyScheduleStudy(paramMap, session);
      }
    }

    state = StudyState.in_progress;
  }

  private class ScheduleRunnable implements Runnable
  {
    private int studyId;
    private ParamMap paramMap;

    public ScheduleRunnable (int studyId, ParamMap paramMap)
    {
      this.studyId = studyId;
      this.paramMap = paramMap;
    }

    public void run ()
    {
      Utils.secondsSleep(2);

      Session session = HibernateUtil.getSession();
      Transaction transaction = session.beginTransaction();
      Study study = null;
      try {
        study = (Study) session.get(Study.class, studyId);
        study.actuallyScheduleStudy(paramMap, session);
      }
      catch (Exception e) {
        transaction.rollback();
        e.printStackTrace();
        String msg = "Error Scheduling Study";
        log.error(msg);
        Utils.growlMessage(msg);
      }
      finally {
        if (transaction.wasCommitted()) {
          if (study != null) {
            log.info(String.format("Scheduled Study %s", study.getStudyId()));
          }
        }
        session.close();

        MemStore.updateNameMapping(true);
      }
      log.info("Background Scheduling Study done");
    }
  }

  private void actuallyScheduleStudy (ParamMap paramMap, Session session)
  {
    String startDate =
        Utils.dateToStringFull(Utils.offsetDate()).replace(" ", "_");
    paramMap.setOrUpdateValue(Type.createTime, startDate);

    String seedList = paramMap.getValue(Type.seedList);
    // Create experiments based length of seed-set
    if (!seedList.isEmpty()) {
      List<Integer> seedIds = Seed.retrieveSeeds(seedList);
      if (seedIds == null) {
        Utils.growlMessage("Not scheduling Study, seed list invalid");
        return;
      }

      int multiplier = seedIds.size();

      for (int counter = 1; counter <= multiplier; counter++) {
        int seedId = seedIds.get(counter - 1);
        createExperiment(session, seedId, counter);
      }
    }

    // Create experiments based on multiplier
    else {
      int multiplier = Integer.valueOf(paramMap.getValue(Type.multiplier));
      for (int counter = 1; counter <= multiplier; counter++) {
        createExperiment(session, null, counter);
      }
    }
  }

  private void createExperiment (Session session, Integer seedId, int counter)
  {
    Experiment experiment = new Experiment();
    experiment.setStudy(this);
    experiment.copyParameters(paramMap);
    if (seedId != null) {
      Parameter parameter = new Parameter(experiment, Type.seedId, seedId);
      experiment.getParamMap().put(Type.seedId, parameter);
    }
    session.saveOrUpdate(experiment);
    experiment.createGames(session, counter);

    log.info(String.format("Created experiment: %s", experiment.getExperimentId()));
  }

  public void experimentCompleted (Session session, int finishedExperimentId)
  {
    boolean allDone = true;

    List<Experiment> experiments = Experiment.getAllExperiments(session);

    for (Experiment experiment : experiments) {
      // The state of the finished game isn't in the db yet.
      if (experiment.getStudy().getStudyId() != studyId ||
          experiment.getExperimentId() == finishedExperimentId) {
        continue;
      }
      allDone &= experiment.getState().equals(ExperimentState.complete);
    }

    if (allDone) {
      state = StudyState.complete;
      session.update(this);
    }

    // Always generate new CSVs
    //CSV.createRoundCsv(this);
  }

  // TODO Shouldn't be needed, use default values
  public void ensureParameters (ParamMap setMap, Location location)
  {
    // Guarantee required params
    if (setMap.get(Type.gameLength().name) == null) {
      setMap.put(Type.gameLength().name, new Parameter(this, Type.gameLength,
          Game.computeGameLength()));
    }
    if (setMap.get(Type.location().name) == null) {
      setMap.put(Type.location().name, new Parameter(this, Type.location,
          location.getLocation()));
    }
    if (setMap.get(Type.simStartDate().name) == null) {
      setMap.put(Type.simStartDate().name, new Parameter(this, Type.simStartDate,
          Utils.dateToStringSmall(location.getDateFrom())));
    }
    if (setMap.get(Type.reuseBoot().name) == null) {
      setMap.put(Type.reuseBoot().name, new Parameter(this, Type.reuseBoot, true));
    }

    if (variableName.isEmpty()) {
      return;
    }

    // TODO Check this
    Type type = Type.get(setMap.getPomId(), variableName);
    if (type == null) {
      setMap.remove(variableName);
    }
  }

  public void removeLogFiles ()
  {
    Properties properties = Properties.getProperties();
    String logLoc = properties.getProperty("logLocation");

    for (Experiment experiment : getExperiments()) {
      for (Game game : experiment.getGameMap().values()) {
        String logFilePath = String.format("%s%s.tar.gz",
            logLoc, game.getGameName());
        File file = new File(logFilePath);
        file.delete();

        for (int brokerId : game.getAgentMap().keySet()) {
          String logBrokerPath = String.format("%s%s.broker-%d.tar.gz",
              logLoc, game.getGameName(), brokerId);

          file = new File(logBrokerPath);
          file.delete();
        }
      }
    }
  }

  //<editor-fold desc="Collections">
  @SuppressWarnings("unchecked")
  public static List<Study> getAllStudies ()
  {
    List<Study> studies = new ArrayList<>();

    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      studies = (List<Study>) session
          .createQuery(Constants.HQL.GET_STUDIES)
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return studies;
  }

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "studyId")
  @MapKey(name = "type")
  private Map<String, Parameter> getParameterMap ()
  {
    return parameterMap;
  }

  private void setParameterMap (Map<String, Parameter> parameterMap)
  {
    this.parameterMap = parameterMap;
    paramMap = new ParamMap(this, parameterMap);
  }

  @Transient
  public ParamMap getParamMap ()
  {
    return paramMap;
  }
  //</editor-fold>

  //<editor-fold desc="Setters and Getters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "studyId", unique = true, nullable = false)
  public int getStudyId ()
  {
    return studyId;
  }

  public void setStudyId (int studyId)
  {
    this.studyId = studyId;
  }

  @ManyToOne
  @JoinColumn(name = "userId")
  public User getUser ()
  {
    return user;
  }

  public void setUser (User user)
  {
    this.user = user;
  }

  @Column(name = "name", nullable = false)
  public String getName ()
  {
    return name;
  }

  public void setName (String name)
  {
    this.name = name;
  }

  @Column(name = "state", nullable = false)
  @Enumerated(EnumType.STRING)
  public StudyState getState ()
  {
    return state;
  }

  public void setState (StudyState state)
  {
    this.state = state;
  }

  @Column(name = "variableName", nullable = true)
  public String getVariableName ()
  {
    return variableName;
  }

  public void setVariableName (String variableName)
  {
    this.variableName = variableName;
  }

  @Column(name = "variableValue", nullable = true)
  public String getVariableValue ()
  {
    return variableValue;
  }

  public void setVariableValue (String variableValue)
  {
    this.variableValue = variableValue;
  }
//</editor-fold>
}
